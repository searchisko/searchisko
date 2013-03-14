/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.persistence.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.jboss.dcp.api.DcpContentObjectFields;
import org.jboss.dcp.api.testtools.TestUtils;
import org.jboss.dcp.api.util.SearchUtils;
import org.jboss.dcp.persistence.service.ContentPersistenceService.ListRequest;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test for {@link JpaHibernateContentPersistenceService}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class JpaHibernateContentPersistenceServiceTest extends JpaTestBase {

	{
		logger = Logger.getLogger(JpaHibernateContentPersistenceServiceTest.class.getName());
	}

	@Test
	public void checkAndEnsureTableExists() {
		JpaHibernateContentPersistenceService tested = getTested();

		try {
			em.getTransaction().begin();

			Assert.assertFalse(tested.checkTableExists("table1"));
			Assert.assertFalse(tested.checkTableExists("table1"));

			tested.ensureTableExists("table1");
			Assert.assertTrue(tested.checkTableExists("table1"));

			em.getTransaction().commit();

			em.getTransaction().begin();
			tested.ensureTableExists("table1");
			Assert.assertTrue(tested.checkTableExists("table1"));
			Assert.assertTrue(tested.checkTableExists("table1"));

			Assert.assertFalse(tested.checkTableExists("table_2"));
			tested.ensureTableExists("table_2");
			Assert.assertTrue(tested.checkTableExists("table_2"));
			Assert.assertTrue(tested.checkTableExists("table1"));
			em.getTransaction().commit();
		} catch (Exception ex) {
			em.getTransaction().rollback();
			ex.printStackTrace();
			Assert.fail("Exception during testPersistence");
		}
	}

	@Test
	public void store_get_delete() {
		JpaHibernateContentPersistenceService tested = getTested();

		try {
			em.getTransaction().begin();

			// case - get from noexisting table
			Assert.assertNull(tested.get("a-1", "tt"));

			String dcpContentType = "testtype_1";
			Map<String, Object> content = null;
			// case - store into nonexisting table, nonexisting id
			tested.store("aaa-1", dcpContentType, content);

			assertRowCount(tested, dcpContentType, 1);
			Assert.assertNull(tested.get("aaa-1", dcpContentType));

			// case - get nonexisting id from existing table
			Assert.assertNull(tested.get("a-1", dcpContentType));

			em.getTransaction().commit();

			em.getTransaction().begin();
			// case - test persistence after commit
			assertRowCount(tested, dcpContentType, 1);
			Assert.assertNull(tested.get("aaa-1", dcpContentType));

			// case - store into existing table, nonexisting id
			content = new HashMap<String, Object>();
			content.put("testkey", "testvalue");
			tested.store("aaa-2", dcpContentType, content);
			assertRowCount(tested, dcpContentType, 2);
			Assert.assertNull(tested.get("aaa-1", dcpContentType));
			TestUtils.assertJsonContent("{\"testkey\" : \"testvalue\"}", tested.get("aaa-2", dcpContentType));
			em.getTransaction().commit();

			em.getTransaction().begin();
			// case - store into existing table, existing id so update, dcp_updated is Date instance
			content.put(DcpContentObjectFields.DCP_UPDATED, new Date(65463749865l));
			tested.store("aaa-1", dcpContentType, content);
			assertRowCount(tested, dcpContentType, 2);
			TestUtils.assertJsonContent("{\"testkey\" : \"testvalue\", \"dcp_updated\":\"1972-01-28T16:22:29.865+0000\"}",
					tested.get("aaa-1", dcpContentType));
			assertTableContent(tested, dcpContentType, "aaa-1",
					SearchUtils.getISODateFormat().parse("1972-01-28T16:22:29.865+0000"));
			// case - store into existing table, existing id so update, dcp_updated is ISO String instance
			content.put(DcpContentObjectFields.DCP_UPDATED, "1973-01-28T17:22:29.865+0100");
			tested.store("aaa-2", dcpContentType, content);
			assertRowCount(tested, dcpContentType, 2);
			TestUtils.assertJsonContent("{\"testkey\" : \"testvalue\", \"dcp_updated\":\"1973-01-28T17:22:29.865+0100\"}",
					tested.get("aaa-2", dcpContentType));
			assertTableContent(tested, dcpContentType, "aaa-2",
					SearchUtils.getISODateFormat().parse("1973-01-28T17:22:29.865+0100"));

			// case - store into existing table, existing id so update, dcp_updated is invalid String instance but no
			// exception and table is correctly filled
			content.put(DcpContentObjectFields.DCP_UPDATED, "sdfasdf");
			tested.store("aaa-2", dcpContentType, content);
			assertRowCount(tested, dcpContentType, 2);
			TestUtils.assertJsonContent("{\"testkey\" : \"testvalue\", \"dcp_updated\":\"sdfasdf\"}",
					tested.get("aaa-2", dcpContentType));
			assertTableContent(tested, dcpContentType, "aaa-2", null);

			em.getTransaction().commit();

			em.getTransaction().begin();
			// case - delete from nonexisting table
			tested.delete("aaa", "jj");

			// case - delete from existing table, nonexisting id
			tested.delete("a-1", dcpContentType);
			assertRowCount(tested, dcpContentType, 2);

			// case - delete existing id
			tested.delete("aaa-1", dcpContentType);
			assertRowCount(tested, dcpContentType, 1);
			Assert.assertNull(tested.get("aaa-1", dcpContentType));
			Assert.assertNotNull(tested.get("aaa-2", dcpContentType));

			em.getTransaction().commit();

		} catch (Exception ex) {
			em.getTransaction().rollback();
			ex.printStackTrace();
			Assert.fail("Exception during testPersistence");
		}
	}

	@Test
	public void listRequest() {
		JpaHibernateContentPersistenceService tested = getTested();
		tested.LIST_PAGE_SIZE = 3;
		try {
			String dcpContentType = "testtypelist";

			// case - no table exists for type
			{
				ListRequest req = tested.listRequestInit(dcpContentType);
				Assert.assertFalse(req.hasContent());
			}

			em.getTransaction().begin();
			for (int i = 7; i >= 1; i--)
				addContent(tested, dcpContentType, "aaa-" + i);
			em.getTransaction().commit();

			// case - data handling test
			{
				ListRequest req = tested.listRequestInit(dcpContentType);
				Assert.assertTrue(req.hasContent());
				Assert.assertNotNull(req.content());
				Assert.assertEquals(3, req.content().size());
				Assert.assertEquals("aaa-1", req.content().get(0).get(DcpContentObjectFields.DCP_ID));
				Assert.assertEquals("aaa-2", req.content().get(1).get(DcpContentObjectFields.DCP_ID));
				Assert.assertEquals("aaa-3", req.content().get(2).get(DcpContentObjectFields.DCP_ID));

				req = tested.listRequestNext(req);
				Assert.assertTrue(req.hasContent());
				Assert.assertNotNull(req.content());
				Assert.assertEquals(3, req.content().size());
				Assert.assertEquals("aaa-4", req.content().get(0).get(DcpContentObjectFields.DCP_ID));
				Assert.assertEquals("aaa-5", req.content().get(1).get(DcpContentObjectFields.DCP_ID));
				Assert.assertEquals("aaa-6", req.content().get(2).get(DcpContentObjectFields.DCP_ID));

				req = tested.listRequestNext(req);
				Assert.assertTrue(req.hasContent());
				Assert.assertNotNull(req.content());
				Assert.assertEquals(1, req.content().size());
				Assert.assertEquals("aaa-7", req.content().get(0).get(DcpContentObjectFields.DCP_ID));

				req = tested.listRequestNext(req);
				Assert.assertFalse(req.hasContent());

			}

		} catch (Exception ex) {
			if (em.getTransaction().isActive())
				em.getTransaction().rollback();
			ex.printStackTrace();
			Assert.fail("Exception during testPersistence");
		}

	}

	private void addContent(JpaHibernateContentPersistenceService tested, String dcpContentType, String id) {
		Map<String, Object> content = new HashMap<String, Object>();
		content.put(DcpContentObjectFields.DCP_ID, id);
		content.put(DcpContentObjectFields.DCP_CONTENT_TYPE, dcpContentType);
		content.put(DcpContentObjectFields.DCP_DESCRIPTION, "value " + id);
		tested.store(id, dcpContentType, content);
	}

	private void assertRowCount(JpaHibernateContentPersistenceService tested, String dcpContentType, int expectedCount) {
		final String tablename = tested.getTableName(dcpContentType);
		Assert.assertEquals((Integer) expectedCount, tested.doDatabaseReturningWork(new ReturningWork<Integer>() {

			@Override
			public Integer execute(Connection conn) throws SQLException {
				PreparedStatement ps = null;
				ResultSet rs = null;
				try {
					ps = conn.prepareStatement("select count(*) from " + tablename);
					rs = ps.executeQuery();
					if (rs.next()) {
						return rs.getInt(1);
					}
					return 0;
				} finally {
					JpaHibernateContentPersistenceService.closeJDBCStuff(ps, rs);
				}
			}
		}));
	}

	private void assertTableContent(final JpaHibernateContentPersistenceService tested, final String dcpContentType,
			final String id, final Date expectedUpdated) {
		tested.doDatabaseWork(new Work() {

			@Override
			public void execute(Connection connection) throws SQLException {
				PreparedStatement ps = null;
				ResultSet rs = null;
				try {
					ps = connection.prepareStatement("select dcp_content_type, updated from "
							+ tested.getTableName(dcpContentType) + " where id = ?");
					ps.setString(1, id);
					rs = ps.executeQuery();
					Assert.assertTrue(rs.next());
					Assert.assertEquals(dcpContentType, rs.getString(1));
					Timestamp actualTimestamp = rs.getTimestamp(2);
					if (expectedUpdated != null) {
						Assert.assertEquals(new Timestamp(expectedUpdated.getTime()), actualTimestamp);
					} else {
						Assert.assertNotNull(actualTimestamp);
					}

				} finally {
					JpaHibernateContentPersistenceService.closeJDBCStuff(ps, rs);
				}
			}

		});
	}

	/**
	 * @return
	 */
	protected JpaHibernateContentPersistenceService getTested() {
		JpaHibernateContentPersistenceService tested = new JpaHibernateContentPersistenceService();
		tested.em = em;
		tested.log = Logger.getLogger("test logger");
		return tested;
	}

}
