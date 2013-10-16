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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import javax.sql.DataSource;

import org.searchisko.api.ContentObjectFields;
import org.searchisko.api.util.SearchUtils;

/**
 * Hibernate JPA implementation of {@link ContentPersistenceService}. We use raw JDBC here because we need to
 * dynamically create and use tables for distinct sys_content_types which is not supported in JPA.<br>
 * It's session bean to work with transactions.
 *
 * @author Vlastimil Elias (velias at redhat dot com), Jason Porter (jporter@redhat.com)
 */
@Named
@Stateless
@LocalBean
public class JpaHibernateContentPersistenceService implements ContentPersistenceService {

    @Inject
    protected Logger log;

    @Resource(name = "java:jboss/datasources/SearchiskoDS")
    protected DataSource searchiskoDs;

    @Override
    public Map<String, Object> get(String id, String sysContentType) {
        String tableName = getTableName(sysContentType);
        ensureTableExists(tableName);
        if (!checkTableExists(tableName))
            return null;
        final String sqlString = String.format("select json_data from %s where id = ?", tableName);
        final String jsonData = this.executeStringReturningSql(sqlString, id);
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

        final String selectSql = String.format("select json_data from %s where id = ?", tableName);
        final boolean shouldUpdate = (executeStringReturningSql(selectSql, id) != null);

        if (shouldUpdate) {
            final String updateSql = String.format("update %s set json_data=?, sys_content_type=?, updated=? where id=?",
                    tableName);
            this.executeNonReturningSql(updateSql, jsonString, sysContentType, updated, id);
        } else {
            final String insert = String.format("insert into %s (id, json_data, sys_content_type, updated) values (?, ?, ?, ?)",
                    tableName);
            this.executeNonReturningSql(insert, id, jsonString, sysContentType, updated);
        }
    }

    @Override
    public void delete(String id, String sysContentType) {
        String tableName = getTableName(sysContentType);
        if (!checkTableExists(tableName))
            return;
        this.executeNonReturningSql(String.format("delete from %s where id = ?", tableName), id);
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

    protected static final ConcurrentMap<String, Boolean> TABLES_EXISTS = new ConcurrentHashMap<>(10);

    /**
     * Check if table exists in DB for given table name.
     *
     * @param tableName to check
     * @return true if table exists, false if not
     */
    protected boolean checkTableExists(String tableName) {
        if (TABLES_EXISTS.isEmpty()) {
            final String sql = "select table_name from information_schema.tables where upper(table_schema) <> 'INFORMATION_SCHEMA'";
            List<String> allTables = this.executeListReturningSql(sql);
            for (String table : allTables) {
                TABLES_EXISTS.putIfAbsent(table, Boolean.TRUE);
            }
        }
        return TABLES_EXISTS.containsKey(tableName);
    }

    /**
     * Check if table exists in DB for given table name and create it if not.
     *
     * @param tableName to check/create
     */
    protected synchronized void ensureTableExists(String tableName) {
        if (!checkTableExists(tableName)) {
            this.executeNonReturningSql(String.format("create table %s%s", tableName, TABLE_STRUCTURE_DDL));
            TABLES_EXISTS.put(tableName, Boolean.TRUE);
        }
    }

    private static final String TABLE_STRUCTURE_DDL = " ( id varchar(200) not null primary key, json_data clob, sys_content_type varchar(100) not null, updated timestamp )";

    protected void executeNonReturningSql(final String sql, final Object... params) {
        try (final Connection conn = this.searchiskoDs.getConnection();
             final PreparedStatement statement = conn.prepareStatement(sql)) {
            this.setParams(statement, params);

            statement.execute();
        } catch (SQLException e) {
            log.severe(String.format("Error executing statement -- %s -- Error -- %s", sql, e.getMessage()));
        }
    }

    protected List<String> executeListReturningSql(final String sql, final Object... params) {
        final List<String> returnList = new ArrayList<>(10);
        try (final Connection conn = this.searchiskoDs.getConnection();
             final PreparedStatement statement = conn.prepareStatement(sql)) {
            this.setParams(statement, params);

            try (final ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    returnList.add(rs.getString(1));
                }
            }
            return returnList;
        } catch (SQLException e) {
            log.severe(String.format("Error executing statement -- %s -- Error -- %s", sql, e.getMessage()));
        }
        return returnList;
    }

    protected String executeStringReturningSql(final String sql, final Object... params) {
        try (final Connection conn = this.searchiskoDs.getConnection();
             final PreparedStatement statement = conn.prepareStatement(sql)) {
            setParams(statement, params);

            try (final ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    return rs.getString(1);
                }
            }
        } catch (SQLException e) {
            log.severe(String.format("Error executing statement -- %s -- Error -- %s", sql, e.getMessage()));
        }
        return null;
    }

    private void setParams(PreparedStatement statement, Object... params) throws SQLException {
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

    protected static class JpaListRequest implements ListRequest {

        List<Map<String, Object>> content;
        String sysContentType;
        int beginIndex = 0;

        protected JpaListRequest(String sysContentType, int beginIndex, List<Map<String, Object>> content) {
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
        public List<Map<String, Object>> content() {
            return content;
        }

    }

    @Override
    public ListRequest listRequestInit(String sysContentType) {
        return listRequestImpl(sysContentType, 0);
    }

    @Override
    public ListRequest listRequestNext(ListRequest previous) {
        JpaListRequest lr = (JpaListRequest) previous;
        return listRequestImpl(lr.sysContentType, lr.beginIndex + LIST_PAGE_SIZE);
    }

    protected ListRequest listRequestImpl(String sysContentType, int beginIndex) {
        new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> content = new ArrayList<>(10);
        String tableName = getTableName(sysContentType);
        ensureTableExists(tableName);
        if (checkTableExists(tableName)) {
            final String sql = String.format("select json_data, id from %s order by id limit %d offset %d",
                    tableName, LIST_PAGE_SIZE, beginIndex);
            final List<String> jsonDataList = this.executeListReturningSql(sql);
            for (final String data : jsonDataList) {
                try {
                    content.add(SearchUtils.convertToJsonMap(data));
                } catch (IOException e) {
                    this.log.severe("Could not convert JSON to valid object, " + e.getMessage());
                }
            }
        }
        return new JpaListRequest(sysContentType, beginIndex, content);
    }
}
