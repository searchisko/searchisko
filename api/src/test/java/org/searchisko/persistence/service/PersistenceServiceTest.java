/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.persistence.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * @author Lukas Vlcek
 */
public class PersistenceServiceTest extends JpaTestBase {

	private static PersistenceService tested;

	@Test
	public void getCountsFromTables() {

		Map<String, Object> data1 = new HashMap<>();
		data1.put("key1", "value1");

		PersistenceService tested = getTested();
		tested.jdbcContentPersistenceService.store("id1", "type1", data1);
		Map<String, Object> result = tested.getTableCounts();

		// Include default tables
		Assert.assertEquals(1 + 6, result.keySet().size());

		String tableName = "data_type1";
		Assert.assertEquals(1, result.get(tableName));

		clearDatabase();
	}

	protected PersistenceService getTested() {
		try {
			DataSource ds = Mockito.mock(DataSource.class);
			Mockito.when(ds.getConnection()).then(new Answer<Connection>() {
				@Override
				public Connection answer(InvocationOnMock invocation) throws Throwable {
					return getConnectionProvider().getConnection();
				}
			});
			tested.jdbcContentPersistenceService.searchiskoDs = ds;
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		return tested;
	}

	@BeforeClass
	public static void beforeClass() {
		JdbcContentPersistenceService jdbcContentPersistenceService = new JdbcContentPersistenceService();
		jdbcContentPersistenceService.log = Logger.getLogger("test logger");

		tested = new PersistenceService();
		tested.jdbcContentPersistenceService = jdbcContentPersistenceService;
	}


	/**
	 * Drop all tables found in JdbcContentPersistenceService.TABLES_EXISTS
	 * map after each test and also clear this map itself.
	 */
	public void clearDatabase() {
		try {
			final Connection conn = this.getTested().jdbcContentPersistenceService.searchiskoDs.getConnection();
			Set<String> tables = JdbcContentPersistenceService.TABLES_EXISTS.keySet();
			for (String table : tables) {
				conn.prepareStatement("drop table " + table).execute();
			}
			JdbcContentPersistenceService.TABLES_EXISTS.clear();
		} catch (SQLException e) {
			Assert.fail(e.getMessage());
		}
	}

	@AfterClass
	public static void afterClass() {
		tested.jdbcContentPersistenceService = null;
		tested = null;
	}

}
