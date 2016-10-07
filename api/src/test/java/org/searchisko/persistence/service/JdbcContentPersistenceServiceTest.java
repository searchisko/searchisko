/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.persistence.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.searchisko.api.ContentObjectFields;
import org.searchisko.api.testtools.TestUtils;
import org.searchisko.api.util.SearchUtils;

/**
 * Unit test for {@link JdbcContentPersistenceService}
 *
 * @author Vlastimil Elias (velias at redhat dot com)
 * @author Lukas Vlcek
 */
public class JdbcContentPersistenceServiceTest extends JpaTestBase {

	@Test
	public void tableNameCaseDoesNotMatter() {
		JdbcContentPersistenceService.ConcurrentUpperCaseHashMap map = new JdbcContentPersistenceService.ConcurrentUpperCaseHashMap(10);

		map.put("lower", true);

		Assert.assertTrue(map.containsKey("lower"));
		Assert.assertTrue(map.containsKey("Lower"));
		Assert.assertTrue(map.containsKey("LOWER"));

		Assert.assertTrue(map.get("lower"));
		Assert.assertTrue(map.get("Lower"));
		Assert.assertTrue(map.get("LOWER"));

		map.putIfAbsent("UPPER", true);

		Assert.assertTrue(map.containsKey("upper"));
		Assert.assertTrue(map.containsKey("Upper"));
		Assert.assertTrue(map.containsKey("UPPER"));

		Assert.assertTrue(map.get("upper"));
		Assert.assertTrue(map.get("Upper"));
		Assert.assertTrue(map.get("UPPER"));

		Set<String> keys = map.keySet();
		Assert.assertEquals(2, keys.size());
		Assert.assertTrue(keys.contains("LOWER"));
		Assert.assertTrue(keys.contains("UPPER"));

		clearDatabase();
	}

	@Test
	public void checkAndEnsureTableExists() {
		JdbcContentPersistenceService tested = getTested();

		Assert.assertFalse(tested.checkTableExists("table1"));
		Assert.assertFalse(tested.checkTableExists("table1"));

		tested.ensureTableExists("table1");
		Assert.assertTrue(tested.checkTableExists("table1"));

		tested.ensureTableExists("table1");
		Assert.assertTrue(tested.checkTableExists("table1"));
		Assert.assertTrue(tested.checkTableExists("table1"));

		Assert.assertFalse(tested.checkTableExists("table_2"));
		tested.ensureTableExists("table_2");
		Assert.assertTrue(tested.checkTableExists("table_2"));
		Assert.assertTrue(tested.checkTableExists("table1"));

		clearDatabase();
	}

	@Test
	public void checkAndEnsureTableExistsWithCasing() {
		JdbcContentPersistenceService tested = getTested();

		Assert.assertFalse(tested.checkTableExists("table1"));
		Assert.assertFalse(tested.checkTableExists("TABLE1"));

		tested.ensureTableExists("table1");
		Assert.assertTrue(tested.checkTableExists("TABLE1"));
		Assert.assertTrue(tested.checkTableExists("table1"));

		tested.ensureTableExists("TABLE1");
		Assert.assertTrue(tested.checkTableExists("table1"));
		Assert.assertTrue(tested.checkTableExists("TABLE1"));

		Assert.assertFalse(tested.checkTableExists("TABLE_2"));
		tested.ensureTableExists("table_2");
		Assert.assertTrue(tested.checkTableExists("TABLE_2"));
		Assert.assertTrue(tested.checkTableExists("TABLE1"));

		clearDatabase();
	}

	@Test
	public void simulateClusterTableCreation() {
		JdbcContentPersistenceService tested = getTested();

		Assert.assertFalse(tested.checkTableExists("table1"));
		createTable("TABLE1");
		Assert.assertTrue(tested.checkTableExists("table1"));

		clearDatabase();
	}

	@Test
	public void store_get_delete() throws Exception {
		JdbcContentPersistenceService tested = getTested();

		// case - get from noexisting table
		Assert.assertNull(tested.get("a-1", "tt"));

		String sysContentType = "testtype_1";
		Map<String, Object> content = null;
		// case - store into nonexisting table, nonexisting id
		tested.store("aaa-1", sysContentType, content);

		assertRowCount(tested, sysContentType, 1);
		Assert.assertNull(tested.get("aaa-1", sysContentType));

		// case - get nonexisting id from existing table
		Assert.assertNull(tested.get("a-1", sysContentType));

		// case - test persistence after commit
		assertRowCount(tested, sysContentType, 1);
		Assert.assertNull(tested.get("aaa-1", sysContentType));

		// case - store into existing table, nonexisting id
		content = new HashMap<String, Object>();
		content.put("testkey", "testvalue");
		tested.store("aaa-2", sysContentType, content);
		assertRowCount(tested, sysContentType, 2);
		Assert.assertNull(tested.get("aaa-1", sysContentType));
		TestUtils.assertJsonContent("{\"testkey\" : \"testvalue\"}", tested.get("aaa-2", sysContentType));

		// case - store into existing table, existing id so update, sys_updated is Date instance
		content.put(ContentObjectFields.SYS_UPDATED, new Date(65463750000l));
		tested.store("aaa-1", sysContentType, content);
		assertRowCount(tested, sysContentType, 2);
		TestUtils.assertJsonContent("{\"testkey\" : \"testvalue\", \"sys_updated\":\"1972-01-28T16:22:30.000Z\"}",
				tested.get("aaa-1", sysContentType));
		assertTableContent(tested, sysContentType, "aaa-1",
				SearchUtils.getISODateFormat().parse("1972-01-28T16:22:30.000+0000"));
		// case - store into existing table, existing id so update, sys_updated is ISO String instance
		content.put(ContentObjectFields.SYS_UPDATED, "1973-01-28T17:22:30.000+0100");
		tested.store("aaa-2", sysContentType, content);
		assertRowCount(tested, sysContentType, 2);
		TestUtils.assertJsonContent("{\"testkey\" : \"testvalue\", \"sys_updated\":\"1973-01-28T17:22:30.000+0100\"}",
				tested.get("aaa-2", sysContentType));
		assertTableContent(tested, sysContentType, "aaa-2",
				SearchUtils.getISODateFormat().parse("1973-01-28T17:22:30.000+0100"));

		// case - store into existing table, existing id so update, sys_updated is invalid String instance but no
		// exception and table is correctly filled
		content.put(ContentObjectFields.SYS_UPDATED, "sdfasdf");
		tested.store("aaa-2", sysContentType, content);
		assertRowCount(tested, sysContentType, 2);
		TestUtils.assertJsonContent("{\"testkey\" : \"testvalue\", \"sys_updated\":\"sdfasdf\"}",
				tested.get("aaa-2", sysContentType));
		assertTableContent(tested, sysContentType, "aaa-2", null);

		// case - delete from nonexisting table
		tested.delete("aaa", "jj");

		// case - delete from existing table, nonexisting id
		tested.delete("a-1", sysContentType);
		assertRowCount(tested, sysContentType, 2);

		// case - delete existing id
		tested.delete("aaa-1", sysContentType);
		assertRowCount(tested, sysContentType, 1);
		Assert.assertNull(tested.get("aaa-1", sysContentType));
		Assert.assertNotNull(tested.get("aaa-2", sysContentType));

		clearDatabase();
	}

	@Test
	public void listRequest() {
		JdbcContentPersistenceService tested = getTested();
		tested.LIST_PAGE_SIZE = 3;

		String sysContentType = "testtypelist";

		// case - no table exists for type
		{
			ListRequest req = tested.listRequestInit(sysContentType);
			Assert.assertFalse(req.hasContent());
		}

		// case - data handling test
		{

			// store in reverse order to see if listing uses correct ordering
			for (int i = 7; i >= 1; i--)
				addContent(tested, sysContentType, "aaa-" + i);

			ListRequest req = tested.listRequestInit(sysContentType);
			Assert.assertTrue(req.hasContent());
			Assert.assertNotNull(req.content());
			Assert.assertEquals(3, req.content().size());
			Assert.assertEquals("aaa-1", req.content().get(0).getId());
			// assert content is correctly loaded
			Assert.assertEquals("aaa-1", req.content().get(0).getContent().get(ContentObjectFields.SYS_ID));
			Assert.assertEquals("value aaa-1", req.content().get(0).getContent().get(ContentObjectFields.SYS_DESCRIPTION));
			// assert id only for others
			Assert.assertEquals("aaa-2", req.content().get(1).getId());
			Assert.assertEquals("aaa-3", req.content().get(2).getId());

			req = tested.listRequestNext(req);
			Assert.assertTrue(req.hasContent());
			Assert.assertNotNull(req.content());
			Assert.assertEquals(3, req.content().size());
			Assert.assertEquals("aaa-4", req.content().get(0).getId());
			Assert.assertEquals("aaa-5", req.content().get(1).getId());
			Assert.assertEquals("aaa-6", req.content().get(2).getId());

			req = tested.listRequestNext(req);
			Assert.assertTrue(req.hasContent());
			Assert.assertNotNull(req.content());
			Assert.assertEquals(1, req.content().size());
			Assert.assertEquals("aaa-7", req.content().get(0).getId());

			req = tested.listRequestNext(req);
			Assert.assertFalse(req.hasContent());

		}
		clearDatabase();
	}

	@Test
	public void countRecords() {
		JdbcContentPersistenceService tested = getTested();

		String CT = "count_test_type";

		// case - nonexisting table
		Assert.assertEquals(0, tested.countRecords(CT));

		// case - existing empty table
		tested.ensureTableExists(tested.getTableName(CT));
		Assert.assertEquals(0, tested.countRecords(CT));

		Map<String, Object> content = new HashMap<>();
		tested.store("1", CT, content);
		Assert.assertEquals(1, tested.countRecords(CT));

		tested.store("2", CT, content);
		tested.store("3", CT, content);
		tested.store("asdas", CT, content);
		Assert.assertEquals(4, tested.countRecords(CT));

		clearDatabase();
	}

	private void addContent(JdbcContentPersistenceService tested, String sysContentType, String id) {
		Map<String, Object> content = new HashMap<String, Object>();
		content.put(ContentObjectFields.SYS_ID, id);
		content.put(ContentObjectFields.SYS_CONTENT_TYPE, sysContentType);
		content.put(ContentObjectFields.SYS_DESCRIPTION, "value " + id);
		tested.store(id, sysContentType, content);
	}

	private void assertRowCount(JdbcContentPersistenceService tested, String sysContentType, int expectedCount) {
		final String tablename = tested.getTableName(sysContentType);
		int result = 0;
		try (final Connection conn = this.getTested().searchiskoDs.getConnection();
			 final PreparedStatement statement = conn.prepareStatement(String.format("select count(*) from %s", tablename));
			 final ResultSet rs = statement.executeQuery()) {
			while (rs.next()) {
				result = rs.getInt(1);
			}
			Assert.assertEquals(expectedCount, result);
		} catch (SQLException e) {
			Assert.fail(e.getMessage());
		}
	}

	private void assertTableContent(final JdbcContentPersistenceService tested, final String sysContentType,
									final String id, final Date expectedUpdated) {
		final String tablename = tested.getTableName(sysContentType);

		try (final Connection conn = this.getTested().searchiskoDs.getConnection();
			 final PreparedStatement statement = conn.prepareStatement(String.format(
					 "select sys_content_type, updated from %s where id = ?", tablename))) {
			statement.setString(1, id);
			try (final ResultSet rs = statement.executeQuery()) {
				Assert.assertTrue(rs.next());
				Assert.assertEquals(sysContentType, rs.getString(1));
				Timestamp actualTimestamp = rs.getTimestamp(2);
				if (expectedUpdated != null) {
					Assert.assertEquals(new Timestamp(expectedUpdated.getTime()), actualTimestamp);
				} else {
					Assert.assertNotNull(actualTimestamp);
				}
			}
		} catch (SQLException e) {
			Assert.fail(e.getMessage());
		}
	}

	private static JdbcContentPersistenceService tested;

	protected JdbcContentPersistenceService getTested() {
		try {
			DataSource ds = Mockito.mock(DataSource.class);
			Mockito.when(ds.getConnection()).then(new Answer<Connection>() {
				@Override
				public Connection answer(InvocationOnMock invocation) throws Throwable {
					return getConnectionProvider().getConnection();
				}
			});
			tested.searchiskoDs = ds;
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}

		return tested;
	}

	/**
	 * Directly create a table in the database. This can be used
	 * to simulate a second EAP node creating a new table.
	 *
	 * @param tableName table name to create
	 * @see JdbcContentPersistenceService#ensureTableExists
	 */
	protected void createTable(String tableName) {
		try {
			final Connection conn = this.getTested().searchiskoDs.getConnection();
			conn.prepareStatement("create table " + tableName + " ( column1 INT )").execute();
//			conn.commit();
		} catch (SQLException e) {
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * Drop all tables found in JdbcContentPersistenceService.TABLES_EXISTS
	 * map after each test and also clear this map itself.
	 */
	public void clearDatabase() {
		try {
			final Connection conn = this.getTested().searchiskoDs.getConnection();
			Set<String> tables = JdbcContentPersistenceService.TABLES_EXISTS.keySet();
			for (String table : tables) {
				conn.prepareStatement("drop table if exists " + table).execute();
			}
//			conn.commit();
			JdbcContentPersistenceService.TABLES_EXISTS.clear();
		} catch (SQLException e) {
			Assert.fail(e.getMessage());
		}
	}

	@BeforeClass
	public static void beforeClass() {
		tested = new JdbcContentPersistenceService();
		tested.log = Logger.getLogger("test logger");
	}

}
