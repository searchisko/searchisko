/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.persistence.service;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;

import org.hibernate.JDBCException;
import org.hibernate.Session;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.jboss.dcp.api.DcpContentObjectFields;
import org.jboss.dcp.api.util.SearchUtils;

/**
 * Hibernate JPA implementation of {@link ContentPersistenceService}. We use raw JDBC here because we need to
 * dynamically create and use tables for distinct dcp_content_types which is not supported in JPA.<br>
 * It's session bean to work with transactions.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
@Named
@Stateless
@LocalBean
public class JpaHibernateContentPersistenceService implements ContentPersistenceService {

	@Inject
	protected EntityManager em;

	@Inject
	protected Logger log;

	protected void doDatabaseWork(Work work) {
		em.unwrap(Session.class).doWork(work);
	}

	protected <T> T doDatabaseReturningWork(ReturningWork<T> work) {
		return em.unwrap(Session.class).doReturningWork(work);
	}

	@Override
	public Map<String, Object> get(String id, String dcpContentType) {
		String tableName = getTableName(dcpContentType);
		if (!checkTableExists(tableName))
			return null;
		String jsonData = doDatabaseReturningWork(new GetWork(tableName, id));
		if (SearchUtils.trimToNull(jsonData) != null) {
			try {
				return SearchUtils.convertToJsonMap(jsonData);
			} catch (Exception e) {
				log.warning("Persisted JSON data are not valid for dcp_content_type '" + dcpContentType + "' and id '" + id
						+ "': " + e.getMessage());
			}
		}
		return null;
	}

	protected static class GetWork implements ReturningWork<String> {
		String tableName;
		String id;

		private GetWork(String tableName, String id) {
			this.tableName = tableName;
			this.id = id;
		}

		@Override
		public String execute(Connection connection) throws SQLException {
			PreparedStatement ps = null;
			ResultSet rs = null;
			try {
				ps = connection.prepareStatement("select json_data from " + tableName + " where id = ?");
				ps.setString(1, id);
				rs = ps.executeQuery();
				if (rs != null && rs.next()) {
					return rs.getString(1);
				}
				return null;
			} finally {
				closeJDBCStuff(ps, rs);
			}
		}

	}

	@Override
	public void store(String id, String dcpContentType, Map<String, Object> content) {
		String tableName = getTableName(dcpContentType);
		ensureTableExists(tableName);

		Date updated = null;
		if (content != null) {
			Object o = content.get(DcpContentObjectFields.DCP_UPDATED);
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

		try {
			doDatabaseWork(new InsertWork(tableName, id, dcpContentType, updated, jsonString));
		} catch (JDBCException e) {
			// insert failed so try update
			doDatabaseWork(new UpdateWork(tableName, id, dcpContentType, updated, jsonString));
		}
	}

	protected static class InsertWork implements Work {

		String tableName;
		String id;
		String type;
		Date updated;
		String jsonString;

		private InsertWork(String tableName, String id, String type, Date updated, String jsonString) {
			super();
			this.tableName = tableName;
			this.id = id;
			this.type = type;
			this.updated = updated;
			this.jsonString = jsonString;
		}

		@Override
		public void execute(Connection connection) throws SQLException {
			PreparedStatement ps = null;
			try {
				ps = connection.prepareStatement("insert into " + tableName
						+ " (id,json_data,dcp_content_type,updated) values(?,?,?,?)");
				ps.setString(1, id);
				ps.setString(2, jsonString);
				ps.setString(3, type);
				ps.setTimestamp(4, new Timestamp(updated.getTime()));
				ps.executeUpdate();
			} finally {
				closeJDBCStuff(ps, null);
			}
		}

	}

	protected static class UpdateWork implements Work {

		String tableName;
		String id;
		String type;
		Date updated;
		String jsonString;

		private UpdateWork(String tableName, String id, String type, Date updated, String jsonString) {
			super();
			this.tableName = tableName;
			this.id = id;
			this.type = type;
			this.updated = updated;
			this.jsonString = jsonString;
		}

		@Override
		public void execute(Connection connection) throws SQLException {
			PreparedStatement ps = null;
			try {
				ps = connection.prepareStatement("update " + tableName
						+ " set json_data=?,dcp_content_type=?,updated=? where id =?");
				ps.setString(1, jsonString);
				ps.setString(2, type);
				ps.setTimestamp(3, new Timestamp(updated.getTime()));
				ps.setString(4, id);
				ps.executeUpdate();
			} finally {
				closeJDBCStuff(ps, null);
			}
		}

	}

	@Override
	public void delete(String id, String dcpContentType) {
		String tableName = getTableName(dcpContentType);
		if (!checkTableExists(tableName))
			return;
		doDatabaseWork(new DeleteWork(tableName, id));
	}

	protected static class DeleteWork implements Work {
		String tableName;
		String id;

		private DeleteWork(String tableName, String id) {
			this.tableName = tableName;
			this.id = id;
		}

		@Override
		public void execute(Connection connection) throws SQLException {
			PreparedStatement ps = null;
			try {
				ps = connection.prepareStatement("delete from " + tableName + " where id = ?");
				ps.setString(1, id);
				ps.executeUpdate();
			} finally {
				closeJDBCStuff(ps, null);
			}
		}

	}

	/**
	 * Get table name for given dcp_content_type
	 * 
	 * @param dcpContentType to get table name for
	 * @return name of table.
	 */
	protected String getTableName(String dcpContentType) {
		String tableName = "data_" + dcpContentType;
		return tableName;
	}

	protected static final Map<String, Boolean> TABLES_EXISTS = new HashMap<String, Boolean>();

	/**
	 * Check if table exists in DB for given table name.
	 * 
	 * @param tableName to check
	 * @return true if table exists, false if not
	 */
	protected boolean checkTableExists(String tableName) {
		synchronized (TABLES_EXISTS) {
			Boolean b = TABLES_EXISTS.get(tableName);
			if (b != null) {
				return b;
			}
		}

		Boolean b = doDatabaseReturningWork(new CheckTableExistsWork(tableName));
		if (b != null) {
			synchronized (TABLES_EXISTS) {
				TABLES_EXISTS.put(tableName, b);
			}
		}
		return b;
	}

	protected static class CheckTableExistsWork implements ReturningWork<Boolean> {

		String tableName;

		private CheckTableExistsWork(String tableName) {
			this.tableName = tableName;
		}

		@Override
		public Boolean execute(Connection connection) throws SQLException {
			PreparedStatement ps = null;
			try {
				ps = connection.prepareStatement("select id from " + tableName + " where id = ''");
				ps.execute();
				return true;
			} catch (Exception e) {
				return false;
			} finally {
				closeJDBCStuff(ps, null);
			}
		}

	}

	/**
	 * Check if table exists in DB for given table name and create it if not.
	 * 
	 * @param tableName to check/create
	 */
	protected synchronized void ensureTableExists(String tableName) {
		if (!checkTableExists(tableName)) {
			try {
				doDatabaseWork(new CreateTableWork(tableName));
				synchronized (TABLES_EXISTS) {
					TABLES_EXISTS.put(tableName, Boolean.TRUE);
				}
			} catch (JDBCException e) {
				// in case of exception we are not sure if table exists or not, so recheck it and throw exception out if not
				// created
				synchronized (TABLES_EXISTS) {
					TABLES_EXISTS.remove(tableName);
				}
				if (!checkTableExists(tableName)) {
					throw e;
				}
			}
		}
	}

	private static final String TABLE_STRUCTURE_DDL = " (" + " id varchar(200) not null primary key,"
			+ " json_data clob," + " dcp_content_type varchar(50) not null," + " updated timestamp )";

	protected static class CreateTableWork implements Work {

		String tableName;

		private CreateTableWork(String tableName) {
			this.tableName = tableName;
		}

		@Override
		public void execute(Connection connection) throws SQLException {
			PreparedStatement ps = null;
			try {
				ps = connection.prepareStatement("create table " + tableName + TABLE_STRUCTURE_DDL);
				ps.executeUpdate();
			} finally {
				closeJDBCStuff(ps, null);
			}
		}

	}

	protected static void closeJDBCStuff(PreparedStatement ps, ResultSet rs) {
		if (rs != null) {
			try {
				rs.close();
			} catch (Exception e) {
				// nothing
			}
		}
		if (ps != null) {
			try {
				ps.close();
			} catch (Exception e) {
				// nothing
			}
		}
	}

	protected int LIST_PAGE_SIZE = 1000;

	protected static class JpaListRequest implements ListRequest {

		List<Map<String, Object>> content;
		String dcpContentType;
		int beginIndex = 0;

		protected JpaListRequest(String dcpContentType, int beginIndex, List<Map<String, Object>> content) {
			super();
			this.dcpContentType = dcpContentType;
			this.beginIndex = beginIndex;
			this.content = content;
		}

		@Override
		public boolean hasContent() {
			return content != null && !content.isEmpty();
		}

		@Override
		public List<Map<String, Object>> content() {
			return content;
		}

	}

	@Override
	public ListRequest listRequestInit(String dcpContentType) {
		return listRequestImpl(dcpContentType, 0);
	}

	@Override
	public ListRequest listRequestNext(ListRequest previous) {
		JpaListRequest lr = (JpaListRequest) previous;
		return listRequestImpl(lr.dcpContentType, lr.beginIndex + LIST_PAGE_SIZE);
	}

	protected ListRequest listRequestImpl(String dcpContentType, int beginIndex) {
		new ArrayList<Map<String, Object>>();
		List<Map<String, Object>> content = null;
		String tableName = getTableName(dcpContentType);
		if (checkTableExists(tableName)) {
			content = doDatabaseReturningWork(new ListWork(tableName, beginIndex, LIST_PAGE_SIZE));
		}
		return new JpaListRequest(dcpContentType, beginIndex, content);
	}

	protected static class ListWork implements ReturningWork<List<Map<String, Object>>> {
		String tableName;
		int beginIndex;
		int listPageSize;

		private ListWork(String tableName, int beginIndex, int listPageSize) {
			super();
			this.tableName = tableName;
			this.beginIndex = beginIndex;
			this.listPageSize = listPageSize;
		}

		@Override
		public List<Map<String, Object>> execute(Connection connection) throws SQLException {
			PreparedStatement ps = null;
			ResultSet rs = null;
			String id = null;
			try {
				ps = connection.prepareStatement("select id,json_data from " + tableName + " order by id" + " limit "
						+ listPageSize + " OFFSET " + beginIndex);
				rs = ps.executeQuery();
				List<Map<String, Object>> content = new ArrayList<Map<String, Object>>();
				while (rs != null && rs.next()) {
					id = rs.getString(1);
					content.add(SearchUtils.convertToJsonMap(rs.getString(2)));
				}
				return content;
			} catch (IOException e) {
				throw new SQLException("Persisted JSON data are not valid for DB table '" + tableName + "' and id '" + id
						+ "': " + e.getMessage());
			} finally {
				closeJDBCStuff(ps, rs);
			}
		}
	}

}
