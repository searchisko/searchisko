/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.persistence.service;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.inject.Named;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.sql.DataSource;

import org.searchisko.api.ContentObjectFields;
import org.searchisko.api.util.CdiHelper;
import org.searchisko.api.util.SearchUtils;

/**
 * JDBC based implementation of {@link ContentPersistenceService}. We use raw JDBC here because we dynamically create
 * and use tables for distinct sys_content_types to handle big numbers of documents.<br>
 * It uses select from SQL standard <code>information_schema.tables</code> view to check table existence, which may be
 * incompatible with some DB engines who do not follow SQL exactly (like Oracle)! It also uses <code>LONGTEXT</code>
 * data type for one column, which may be incompatible with some DB engines also.<br>
 * It's session bean to work with transactions.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 * @author Jason Porter (jporter@redhat.com)
 * @author Lukas Vlcek
 */
@Named
@Stateless
@LocalBean
public class JdbcContentPersistenceService implements ContentPersistenceService {

	@Inject
	protected Logger log;

	@Inject
	protected EntityManager em;

	protected DataSource searchiskoDs;

	@PostConstruct
	public void init() throws NamingException {
		searchiskoDs = CdiHelper.getDefaultDataSource(em);
	}

	@Override
	public Map<String, Object> get(String id, String sysContentType) {
		String tableName = getTableName(sysContentType);
		if (!checkTableExists(tableName))
			return null;
		String sqlString = String.format("select json_data from %s where id = ?", tableName);
		String jsonData = executeStringReturningSql(sqlString, id);
		if (SearchUtils.trimToNull(jsonData) != null) {
			try {
				return SearchUtils.convertToJsonMap(jsonData);
			} catch (Exception e) {
				log.warning(String.format("Persisted JSON data are not valid for sys_content_type '%s' and id '%s': %s",
						sysContentType, id, e.getMessage()));
			}
		}
		return null;
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public void store(String id, String sysContentType, Map<String, Object> content) {
		String tableName = getTableName(sysContentType);
		ensureTableExists(tableName);

		Date updated = null;
		if (content != null) {
			Object o = content.get(ContentObjectFields.SYS_UPDATED);
			if (o instanceof Date) {
				updated = (Date) o;
			} else if (o instanceof String) {
				try {
					updated = SearchUtils.dateFromISOString((String) o, true);
				} catch (Exception e) {
					// ignore exception here
				}
			}
		}
		if (updated == null)
			updated = new Date();

		String jsonString;
		try {
			jsonString = SearchUtils.convertJsonMapToString(content);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		try (final Connection conn = searchiskoDs.getConnection()) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Start Store. "
						+ " Auto commit: " + conn.getAutoCommit()
						+ " isolation: " + conn.getTransactionIsolation()
						+ " Db name: " + conn.getMetaData().getDatabaseProductName());
			}
			if (SearchUtils.isMysqlDialect(conn.getMetaData().getDatabaseProductName())) {
				log.log(Level.FINE, "Store data via insert on duplicate key update technique");
				executeNonReturningSql(conn,
						String.format("insert into %s (id, json_data, sys_content_type, updated) values (?, ?, ?, ?) ON DUPLICATE KEY UPDATE json_data=?", tableName),
						id, jsonString, sysContentType, updated, jsonString);
			} else {
				try {
					log.log(Level.FINE, "Try insert data");
					executeNonReturningSql(conn,
							String.format("insert into %s (id, json_data, sys_content_type, updated) values (?, ?, ?, ?)", tableName),
							id, jsonString, sysContentType, updated);
				} catch (SQLException e) {
					// insert failed, so record is in DB already, so we try to upgrade it
					log.log(Level.FINE, "Insert failed. Try update row");
					executeNonReturningSql(conn,
							String.format("update %s set json_data=?, sys_content_type=?, updated=? where id=?", tableName),
							jsonString, sysContentType, updated, id);
				}
			}

			log.log(Level.FINEST, "Store completed");
		} catch (SQLException e) {
			log.severe(String.format("Error while storing content of type '" + sysContentType + "' with id '" + id
					+ "' in the DB -- %s", e.getMessage()));
			throw new RuntimeException(e);
		}

	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public void delete(String id, String sysContentType) {
		String tableName = getTableName(sysContentType);
		if (!checkTableExists(tableName))
			return;
		executeNonReturningSql(String.format("delete from %s where id = ?", tableName), id);
	}

	/**
	 * Get table name for given sys_content_type
	 * 
	 * @param sysContentType to get table name for
	 * @return name of table.
	 */
	protected String getTableName(String sysContentType) {
		return "data_" + sysContentType;
	}

	/**
	 * We need to ensure that keys are always treated as UPPERCASE values.
	 * Note: this class does not correctly implement whole Map API, right
	 * now only used methods are reimplemented to use UPPERCASE values.
	 */
	static class ConcurrentUpperCaseHashMap extends ConcurrentHashMap<String, Boolean> {
		public ConcurrentUpperCaseHashMap(int size) {
			super(size);
		}
		public Boolean putIfAbsent(String key, Boolean value) {
			return super.putIfAbsent(key.toUpperCase(Locale.US), value);
		}
		public Boolean put(String key, Boolean value) {
			return super.put(key.toUpperCase(Locale.US), value);
		}
		public boolean containsKey(Object key) {
			return super.containsKey(((String) key).toUpperCase(Locale.US));
		}
		public Boolean get(Object key) {
			return super.get(((String)key).toUpperCase(Locale.US));
		}
	}

	protected static final ConcurrentUpperCaseHashMap TABLES_EXISTS = new ConcurrentUpperCaseHashMap(10);

	/**
	 * Check if table exists in DB for given table name.
	 *
	 * @param tableName to check
	 * @return true if table exists, false if not
	 */
	protected boolean checkTableExists(String tableName) {
		if (TABLES_EXISTS.isEmpty()) {
			cacheAllTableNames();
		}
		boolean exists = TABLES_EXISTS.containsKey(tableName);
		// Refresh table names map if table now found
		// Double check is important in case of cluster deployment - should happen rarely.
		if (!exists) {
			cacheAllTableNames();
			exists = TABLES_EXISTS.containsKey(tableName);
		}
		return exists;
	}

	private void cacheAllTableNames() {
		for (String table : getAllTableNames()) {
			TABLES_EXISTS.putIfAbsent(table, Boolean.TRUE);
		}
	}

	/**
	 * Get list of all tables (excluding 'INFORMATION_SCHEMA', 'PERFORMANCE_SCHEMA' or 'MYSQL' table if it exists).
	 *
	 * @return list of all tables
	 */
	public List<String> getAllTableNames() {
		String sql = "select table_name from INFORMATION_SCHEMA.tables " +
				"where upper(table_schema) not in ('INFORMATION_SCHEMA', 'PERFORMANCE_SCHEMA', 'MYSQL')";
		return executeListReturningSql(sql);
	}

	/**
	 * Check if table exists in DB for given table name and create it if not.
	 * 
	 * @param tableName to check/create
	 */
	protected void ensureTableExists(String tableName) {
		synchronized (TABLE_STRUCTURE_DDL) {
			if (!checkTableExists(tableName)) {
				executeNonReturningSql(String.format("create table %s%s", tableName, TABLE_STRUCTURE_DDL));
				TABLES_EXISTS.put(tableName, Boolean.TRUE);
			}
		}
	}

	private static final String TABLE_STRUCTURE_DDL = " ( id varchar(200) not null primary key, json_data longtext, sys_content_type varchar(100) not null, updated timestamp )";

	protected void executeNonReturningSql(final String sql, final Object... params) {
		try (final Connection conn = searchiskoDs.getConnection()) {
			executeNonReturningSql(conn, sql, params);
		} catch (SQLException e) {
			log.severe(String.format("Error executing SQL statement -- %s -- Error -- %s", sql, e.getMessage()));
			throw new RuntimeException(e);
		}
	}

	protected void executeNonReturningSql(final Connection conn, final String sql, final Object... params)
			throws SQLException {
		try (final PreparedStatement statement = conn.prepareStatement(sql)) {
			setParams(statement, params);
			statement.execute();
		}
	}

	protected List<String> executeListReturningSql(final String sql, final Object... params) {
		final List<String> returnList = new ArrayList<>(10);
		try (Connection conn = searchiskoDs.getConnection(); PreparedStatement statement = conn.prepareStatement(sql)) {
			setParams(statement, params);

			try (ResultSet rs = statement.executeQuery()) {
				while (rs.next()) {
					returnList.add(rs.getString(1));
				}
			}
			return returnList;
		} catch (SQLException e) {
			log.severe(String.format("Error executing statement -- %s -- Error -- %s", sql, e.getMessage()));
			throw new RuntimeException(e);
		}
	}

	protected String executeStringReturningSql(final String sql, final Object... params) {
		try (Connection conn = searchiskoDs.getConnection(); PreparedStatement statement = conn.prepareStatement(sql)) {
			setParams(statement, params);

			try (final ResultSet rs = statement.executeQuery()) {
				while (rs.next()) {
					return rs.getString(1);
				}
			}
		} catch (SQLException e) {
			log.severe(String.format("Error executing statement -- %s -- Error -- %s", sql, e.getMessage()));
			throw new RuntimeException(e);
		}
		return null;
	}

	protected int executeIntegerReturningSql(final String sql, final Object... params) {
		try (Connection conn = searchiskoDs.getConnection(); PreparedStatement statement = conn.prepareStatement(sql)) {
			setParams(statement, params);

			try (final ResultSet rs = statement.executeQuery()) {
				while (rs.next()) {
					return rs.getInt(1);
				}
			}
		} catch (SQLException e) {
			log.severe(String.format("Error executing statement -- %s -- Error -- %s", sql, e.getMessage()));
			throw new RuntimeException(e);
		}
		return 0;
	}

	private void setParams(PreparedStatement statement, Object... params) throws SQLException {
		if (params == null || params.length == 0)
			return;
		int i = 1;
		for (Object param : params) {
			if (param instanceof String) {
				statement.setString(i, (String) param);
			} else if (param instanceof Date) {
				statement.setTimestamp(i, new Timestamp(((Date) param).getTime()));
			} else if (param instanceof Integer) {
				statement.setInt(i, (Integer) param);
			} else if (param instanceof Long) {
				statement.setLong(i, (Long) param);
			}
			i++;
		}
	}

	protected int LIST_PAGE_SIZE = 1000;

	protected static class ListRequestImpl implements ListRequest {

		List<ContentTuple<String, Map<String, Object>>> content;
		String sysContentType;
		int beginIndex = 0;

		protected ListRequestImpl(String sysContentType, int beginIndex,
				List<ContentTuple<String, Map<String, Object>>> content) {
			super();
			this.sysContentType = sysContentType;
			this.beginIndex = beginIndex;
			this.content = content;
		}

		@Override
		public boolean hasContent() {
			return content != null && !content.isEmpty();
		}

		@Override
		public List<ContentTuple<String, Map<String, Object>>> content() {
			return content;
		}

	}

	@Override
	public ListRequest listRequestInit(String sysContentType) {
		return listRequestImpl(sysContentType, 0);
	}

	@Override
	public ListRequest listRequestNext(ListRequest previous) {
		ListRequestImpl lr = (ListRequestImpl) previous;
		return listRequestImpl(lr.sysContentType, lr.beginIndex + LIST_PAGE_SIZE);
	}

	protected ListRequest listRequestImpl(String sysContentType, int beginIndex) {
		List<ContentTuple<String, Map<String, Object>>> content = new ArrayList<>(10);
		String tableName = getTableName(sysContentType);
		if (checkTableExists(tableName)) {
			final String sql = String.format("select json_data, id from %s order by id limit %d offset %d", tableName,
					LIST_PAGE_SIZE, beginIndex);
			try (Connection conn = searchiskoDs.getConnection(); PreparedStatement statement = conn.prepareStatement(sql)) {
				try (ResultSet rs = statement.executeQuery()) {
					while (rs.next()) {
						String id = rs.getString(2);
						try {
							content.add(new ContentTuple<>(id, SearchUtils.convertToJsonMap(rs.getString(1))));
						} catch (IOException e) {
							log.severe("Could not convert content to JSON object for contentType='" + sysContentType + "' and id='"
									+ id + "' due: " + e.getMessage());
						}
					}
				}
			} catch (SQLException e) {
				log.severe(String.format("Error executing statement '%s' due error %s", sql, e.getMessage()));
				throw new RuntimeException(e);
			}
		}
		return new ListRequestImpl(sysContentType, beginIndex, content);
	}

	public DataSource getDataSource() {
		return searchiskoDs;
	}

	@Override
	public int countRecords(String sysContentType) {
		return rowCount(getTableName(sysContentType));
	}

	/**
	 * Return count of records in the table.
	 * Can throw unchecked exception (special system table names ... etc).
	 *
	 * @param tableName
	 * @return count of records in the table
	 */
	public int rowCount(String tableName) {
		if (!checkTableExists(tableName))
			return 0;
		String sqlString = String.format("select count(*) from %s", tableName);
		return executeIntegerReturningSql(sqlString);
	}

}
