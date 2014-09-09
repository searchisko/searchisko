/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.persistence.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.Test;
import org.searchisko.api.testtools.TestUtils;
import org.searchisko.persistence.jpa.model.Contributor;
import org.searchisko.persistence.jpa.model.ContributorConverter;

/**
 * Unit test for {@link JpaEntityService}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class JpaEntityServiceTest extends JpaTestBase {

	{
		logger = Logger.getLogger(JpaEntityServiceTest.class.getName());
	}

	@Test
	public void create_noid() {
		JpaEntityService<Contributor> tested = new JpaEntityService<Contributor>(em, new ContributorConverter(),
				Contributor.class);
		try {

			em.getTransaction().begin();
			Map<String, Object> entity = new LinkedHashMap<String, Object>();
			entity.put("testkey1", "test \n value 1");
			entity.put("testkey2", "test \t value 2");
			String id = tested.create(entity);
			Assert.assertNotNull(id);

			entity.put("testkey3", "test value 3");
			String id2 = tested.create(entity);
			Assert.assertNotNull(id2);

			em.getTransaction().commit();

			em.getTransaction().begin();
			Contributor c = em.find(Contributor.class, id);
			Assert.assertNotNull(c);
			Assert.assertEquals("{\"testkey1\":\"test \\n value 1\",\"testkey2\":\"test \\t value 2\"}", c.getValue());
			Contributor c2 = em.find(Contributor.class, id2);
			Assert.assertNotNull(c2);
			Assert.assertEquals(
					"{\"testkey1\":\"test \\n value 1\",\"testkey2\":\"test \\t value 2\",\"testkey3\":\"test value 3\"}",
					c2.getValue());
			em.getTransaction().commit();
		} catch (Exception ex) {
			em.getTransaction().rollback();
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			Assert.fail("Exception during testPersistence");
		}

	}

	@Test
	public void create_id() {
		JpaEntityService<Contributor> tested = new JpaEntityService<Contributor>(em, new ContributorConverter(),
				Contributor.class);
		try {

			// case - create new value
			em.getTransaction().begin();
			Map<String, Object> entity = new LinkedHashMap<String, Object>();
			entity.put("testkey1", "test \n value 1");
			entity.put("testkey2", "test \t value 2");
			String id = "10";
			tested.create(id, entity);

			entity.put("testkey3", "test value 3");
			String id2 = "20";
			tested.create(id2, entity);

			em.getTransaction().commit();

			em.getTransaction().begin();
			Contributor c = em.find(Contributor.class, id);
			Assert.assertNotNull(c);
			Assert.assertEquals("{\"testkey1\":\"test \\n value 1\",\"testkey2\":\"test \\t value 2\"}", c.getValue());
			Contributor c2 = em.find(Contributor.class, id2);
			Assert.assertNotNull(c2);
			Assert.assertEquals(
					"{\"testkey1\":\"test \\n value 1\",\"testkey2\":\"test \\t value 2\",\"testkey3\":\"test value 3\"}",
					c2.getValue());
			em.getTransaction().commit();

			// case - update of existing value
			em.getTransaction().begin();
			entity.put("testkey4", "test value 4");
			tested.create(id, entity);
			em.getTransaction().commit();
			em.getTransaction().begin();
			c = em.find(Contributor.class, id);
			Assert.assertNotNull(c);
			Assert
					.assertEquals(
							"{\"testkey1\":\"test \\n value 1\",\"testkey2\":\"test \\t value 2\",\"testkey3\":\"test value 3\",\"testkey4\":\"test value 4\"}",
							c.getValue());
			em.getTransaction().commit();

		} catch (Exception ex) {
			em.getTransaction().rollback();
			ex.printStackTrace();
			Assert.fail("Exception during testPersistence");
		}
	}

	@Test
	public void update() {
		JpaEntityService<Contributor> tested = new JpaEntityService<Contributor>(em, new ContributorConverter(),
				Contributor.class);
		try {

			// case - create new value
			em.getTransaction().begin();
			Map<String, Object> entity = new LinkedHashMap<String, Object>();
			entity.put("testkey1", "test \n value 1");
			entity.put("testkey2", "test \t value 2");
			String id = "gdfgdgadfgearg";
			tested.update(id, entity);

			entity.put("testkey3", "test value 3");
			String id2 = "yuiyujfgrtjdfg";
			tested.update(id2, entity);

			em.getTransaction().commit();

			em.getTransaction().begin();
			Contributor c = em.find(Contributor.class, id);
			Assert.assertNotNull(c);
			Assert.assertEquals("{\"testkey1\":\"test \\n value 1\",\"testkey2\":\"test \\t value 2\"}", c.getValue());
			Contributor c2 = em.find(Contributor.class, id2);
			Assert.assertNotNull(c2);
			Assert.assertEquals(
					"{\"testkey1\":\"test \\n value 1\",\"testkey2\":\"test \\t value 2\",\"testkey3\":\"test value 3\"}",
					c2.getValue());
			em.getTransaction().commit();

			// case - update of existing value
			em.getTransaction().begin();
			entity.put("testkey4", "test value 4");
			tested.update(id, entity);
			em.getTransaction().commit();
			em.getTransaction().begin();
			c = em.find(Contributor.class, id);
			Assert.assertNotNull(c);
			Assert
					.assertEquals(
							"{\"testkey1\":\"test \\n value 1\",\"testkey2\":\"test \\t value 2\",\"testkey3\":\"test value 3\",\"testkey4\":\"test value 4\"}",
							c.getValue());
			em.getTransaction().commit();

		} catch (Exception ex) {
			em.getTransaction().rollback();
			ex.printStackTrace();
			Assert.fail("Exception during testPersistence");
		}
	}

	@Test
	public void delete() {
		JpaEntityService<Contributor> tested = new JpaEntityService<Contributor>(em, new ContributorConverter(),
				Contributor.class);
		try {

			// case - delete existing
			em.getTransaction().begin();
			String id1 = "aaa";
			String val1 = "DDDDFGHDFHD";
			Contributor c1 = createEntity(id1, val1);
			em.persist(c1);

			String id2 = "bbbbbb";
			String val2 = "fgdsafgdsafgsdf";
			Contributor c2 = createEntity(id2, val2);
			em.persist(c2);
			em.getTransaction().commit();

			em.getTransaction().begin();
			Assert.assertNotNull(em.find(Contributor.class, id1));
			Assert.assertNotNull(em.find(Contributor.class, id2));
			tested.delete(id1);
			em.getTransaction().commit();

			em.getTransaction().begin();
			Assert.assertNull(em.find(Contributor.class, id1));
			Assert.assertNotNull(em.find(Contributor.class, id2));
			em.getTransaction().commit();

			// case - delete unknown
			em.getTransaction().begin();
			tested.delete("unknown");
			if (em.getTransaction().getRollbackOnly()) {
				em.getTransaction().rollback();
			} else {
				em.getTransaction().commit();
			}

		} catch (Exception ex) {
			ex.printStackTrace();
			em.getTransaction().rollback();
			Assert.fail("Exception during testPersistence");
		}
	}

	@Test
	public void getOneAndAllRaw() {
		JpaEntityService<Contributor> tested = new JpaEntityService<Contributor>(em, new ContributorConverter(),
				Contributor.class);
		try {

			em.getTransaction().begin();
			String id1 = "aaa";
			String val1 = "DDDDFGHDFHD";
			Contributor c1 = createEntity(id1, val1);
			em.persist(c1);

			String id2 = "bbbbbb";
			String val2 = "fgdsafgdsafgsdf";
			Contributor c2 = createEntity(id2, val2);
			em.persist(c2);
			em.getTransaction().commit();

			em.getTransaction().begin();
			Map<String, Object> v1 = tested.get(id1);
			Assert.assertNotNull(v1);
			Assert.assertEquals(val1, v1.get("val"));
			Map<String, Object> v2 = tested.get(id2);
			Assert.assertNotNull(v2);
			Assert.assertEquals(val2, v2.get("val"));
			em.getTransaction().commit();

			em.getTransaction().begin();
			List<Map<String, Object>> all = tested.getAll();
			Assert.assertEquals(2, all.size());
			Assert.assertEquals(val1, all.get(0).get("val"));
			Assert.assertEquals(val2, all.get(1).get("val"));
			em.getTransaction().commit();

		} catch (Exception ex) {
			em.getTransaction().rollback();
			ex.printStackTrace();
			Assert.fail("Exception during testPersistence");
		}
	}

	@Test
	public void getAll_pager() {
		JpaEntityService<Contributor> tested = new JpaEntityService<Contributor>(em, new ContributorConverter(),
				Contributor.class);
		try {

			em.getTransaction().begin();
			String id1 = "aaa";
			String val1 = "DDDDFGHDFHD";
			Contributor c1 = createEntity(id1, val1);
			em.persist(c1);

			String id2 = "bbbbbb";
			String val2 = "fgdsafgdsafgsdf";
			Contributor c2 = createEntity(id2, val2);
			em.persist(c2);
			em.getTransaction().commit();

			em.getTransaction().begin();
			// case - no pager used so all returned
			TestUtils
					.assetJsonStreamingOutputContent(
							"{\"total\":2,\"hits\":[{\"id\":\"aaa\",\"data\":{\"val\":\"DDDDFGHDFHD\"}},{\"id\":\"bbbbbb\",\"data\":{\"val\":\"fgdsafgdsafgsdf\"}}]}",
							tested.getAll(null, null, null));

			// persist in changed order to see ordering of query in effect
			em.persist(createEntity("dd", "ddd"));
			em.persist(createEntity("cc", "ccc"));
			em.getTransaction().commit();

			em.getTransaction().begin();
			// case - pager used
			TestUtils.assetJsonStreamingOutputContent(
					"{\"total\":4,\"hits\":[{\"id\":\"aaa\",\"data\":{\"val\":\"DDDDFGHDFHD\"}}]}", tested.getAll(0, 1, null));

			TestUtils
					.assetJsonStreamingOutputContent(
							"{\"total\":4,\"hits\":[{\"id\":\"bbbbbb\",\"data\":{\"val\":\"fgdsafgdsafgsdf\"}},{\"id\":\"cc\",\"data\":{\"val\":\"ccc\"}}]}",
							tested.getAll(1, 2, null));

			TestUtils.assetJsonStreamingOutputContent("{\"total\":4,\"hits\":[]}", tested.getAll(6, 10, null));

			// case - filtering used and strange bounds for pager
			TestUtils
					.assetJsonStreamingOutputContent(
							"{\"total\":4,\"hits\":[{\"id\":\"aaa\",\"data\":{}},{\"id\":\"bbbbbb\",\"data\":{}},{\"id\":\"cc\",\"data\":{}},{\"id\":\"dd\",\"data\":{}}]}",
							tested.getAll(-10, -1, new String[]{"val"}));

			em.getTransaction().commit();

		} catch (Exception ex) {
			em.getTransaction().rollback();
			ex.printStackTrace();
			Assert.fail("Exception during testPersistence");
		}
	}

	@Test
	public void listRequest() {
		JpaEntityService<Contributor> tested = new JpaEntityService<Contributor>(em, new ContributorConverter(),
				Contributor.class);
		tested.LIST_PAGE_SIZE = 3;
		try {

			// case - no table exists for type
			{
				ListRequest req = tested.listRequestInit();
				Assert.assertFalse(req.hasContent());
			}

			// case - data handling test
			{
				em.getTransaction().begin();
				// persist in changed order to see ordering of query in effect
				for (int i = 7; i >= 1; i--) {
					em.persist(createEntity("aaa-" + i, "value-" + i));
				}
				em.getTransaction().commit();

				em.getTransaction().begin();
				ListRequest req = tested.listRequestInit();
				em.getTransaction().commit();
				Assert.assertTrue(req.hasContent());
				Assert.assertNotNull(req.content());
				Assert.assertEquals(3, req.content().size());
				Assert.assertEquals("aaa-1", req.content().get(0).getId());
				Assert.assertEquals("aaa-2", req.content().get(1).getId());
				Assert.assertEquals("aaa-3", req.content().get(2).getId());

				em.getTransaction().begin();
				req = tested.listRequestNext(req);
				em.getTransaction().commit();
				Assert.assertTrue(req.hasContent());
				Assert.assertNotNull(req.content());
				Assert.assertEquals(3, req.content().size());
				Assert.assertEquals("aaa-4", req.content().get(0).getId());
				Assert.assertEquals("aaa-5", req.content().get(1).getId());
				Assert.assertEquals("aaa-6", req.content().get(2).getId());

				em.getTransaction().begin();
				req = tested.listRequestNext(req);
				em.getTransaction().commit();
				Assert.assertTrue(req.hasContent());
				Assert.assertNotNull(req.content());
				Assert.assertEquals(1, req.content().size());
				Assert.assertEquals("aaa-7", req.content().get(0).getId());

				em.getTransaction().begin();
				req = tested.listRequestNext(req);
				em.getTransaction().commit();
				Assert.assertFalse(req.hasContent());

			}

		} catch (Exception ex) {
			ex.printStackTrace();
			Assert.fail("Exception during testPersistence");
		}

	}

	private Contributor createEntity(String id, String value) {
		Contributor c2 = new Contributor();
		c2.setId(id);
		c2.setValue("{\"val\":\"" + value + "\"}");
		return c2;
	}

}
