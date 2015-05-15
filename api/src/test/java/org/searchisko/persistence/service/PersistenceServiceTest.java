/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.persistence.service;

import org.h2.jdbcx.JdbcConnectionPool;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

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
		Map<String, Integer> result = tested.getTableCounts();

		Assert.assertEquals(1, result.keySet().size());
		String tableName = (String) result.keySet().toArray()[0];
		Assert.assertEquals(new Integer(1), result.get(tableName));
	}

	protected PersistenceService getTested() {
		return tested;
	}

	@BeforeClass
	public static void beforeClass() {
		JdbcContentPersistenceService jdbcContentPersistenceService = new JdbcContentPersistenceService();
		jdbcContentPersistenceService.log = Logger.getLogger("test logger");
		jdbcContentPersistenceService.searchiskoDs = JdbcConnectionPool.create("jdbc:h2:mem:unit-testing-jpa-persistence-service-test", "sa", "");

		tested = new PersistenceService();
		tested.jdbcContentPersistenceService = jdbcContentPersistenceService;
	}

	@AfterClass
	public static void afterClass() {
		((JdbcConnectionPool) tested.jdbcContentPersistenceService.searchiskoDs).dispose();
		tested.jdbcContentPersistenceService = null;
		tested = null;
	}

}
