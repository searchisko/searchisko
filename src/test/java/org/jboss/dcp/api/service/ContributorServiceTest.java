/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.service;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.core.StreamingOutput;

import junit.framework.Assert;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.jboss.dcp.api.rest.ESDataOnlyResponse;
import org.jboss.dcp.api.testtools.ESRealClientTestBase;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit test for {@link ContributorService}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ContributorServiceTest extends ESRealClientTestBase {

	private ContributorService getTested(Client client) {
		if (client == null)
			client = Mockito.mock(Client.class);

		ContributorService ret = new ContributorService();
		ret.entityService = Mockito.mock(EntityService.class);
		ret.searchClientService = Mockito.mock(SearchClientService.class);
		Mockito.when(ret.searchClientService.getClient()).thenReturn(client);
		ret.log = Logger.getLogger("testlogger");
		return ret;
	}

	@Test
	public void getAll() {
		ContributorService tested = getTested(null);

		String[] ff = new String[] { "aa" };
		// case - value is returned
		StreamingOutput value = new ESDataOnlyResponse(null);
		Mockito.when(tested.entityService.getAll(10, 20, ff)).thenReturn(value);
		Assert.assertEquals(value, tested.getAll(10, 20, ff));

		// case - null is returned
		Mockito.reset(tested.entityService);
		Mockito.when(tested.entityService.getAll(10, 20, ff)).thenReturn(null);
		Assert.assertEquals(null, tested.getAll(10, 20, ff));
		Mockito.verify(tested.entityService).getAll(10, 20, ff);
		Mockito.verifyNoMoreInteractions(tested.entityService);

		// case - exception is passed
		Mockito.reset(tested.entityService);
		Mockito.when(tested.entityService.getAll(10, 20, ff)).thenThrow(new RuntimeException("testex"));
		try {
			tested.getAll(10, 20, ff);
			Assert.fail("RuntimeException expected");
		} catch (RuntimeException e) {
			// OK
		}
		Mockito.verify(tested.entityService).getAll(10, 20, ff);
		Mockito.verifyNoMoreInteractions(tested.entityService);
	}

	@Test
	public void get() {
		ContributorService tested = getTested(null);

		// case - value is returned
		Map<String, Object> value = new HashMap<String, Object>();
		Mockito.when(tested.entityService.get("10")).thenReturn(value);
		Assert.assertEquals(value, tested.get("10"));

		// case - null is returned
		Mockito.reset(tested.entityService);
		Mockito.when(tested.entityService.get("10")).thenReturn(null);
		Assert.assertEquals(null, tested.get("10"));
		Mockito.verify(tested.entityService).get("10");
		Mockito.verifyNoMoreInteractions(tested.entityService);

		// case - exception is passed
		Mockito.reset(tested.entityService);
		Mockito.when(tested.entityService.get("10")).thenThrow(new RuntimeException("testex"));
		try {
			tested.get("10");
			Assert.fail("RuntimeException expected");
		} catch (RuntimeException e) {
			// OK
		}
		Mockito.verify(tested.entityService).get("10");
		Mockito.verifyNoMoreInteractions(tested.entityService);
	}

	@Test
	public void create_noid() {
		Client client = prepareESClientForUnitTest();
		ContributorService tested = getTested(client);
		try {

			// case - insert to noexisting index
			{
				Map<String, Object> entity = new HashMap<String, Object>();
				entity.put("name", "v1");
				Mockito.when(tested.entityService.create(entity)).thenReturn("1");

				String id = tested.create(entity);
				Assert.assertEquals("1", id);
				Map<String, Object> r = indexGetDocument(ContributorService.SEARCH_INDEX_NAME,
						ContributorService.SEARCH_INDEX_TYPE, "1");
				Assert.assertNotNull(r);
				Assert.assertEquals("v1", r.get("name"));
			}

			// case - insert to existing index
			{
				Map<String, Object> entity = new HashMap<String, Object>();
				entity.put("name", "v2");
				Mockito.when(tested.entityService.create(entity)).thenReturn("2");

				String id = tested.create(entity);
				Assert.assertEquals("2", id);
				Map<String, Object> r = indexGetDocument(ContributorService.SEARCH_INDEX_NAME,
						ContributorService.SEARCH_INDEX_TYPE, "2");
				Assert.assertNotNull(r);
				Assert.assertEquals("v2", r.get("name"));
				r = indexGetDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, "1");
				Assert.assertNotNull(r);
				Assert.assertEquals("v1", r.get("name"));

			}

		} finally {
			indexDelete(ContributorService.SEARCH_INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	@Test
	public void create_id() {
		Client client = prepareESClientForUnitTest();
		ContributorService tested = getTested(client);
		try {

			// case - insert noexisting object to noexisting index
			{
				Map<String, Object> entity = new HashMap<String, Object>();
				entity.put("name", "v1");

				tested.create("1", entity);
				Mockito.verify(tested.entityService).create("1", entity);
				Map<String, Object> r = indexGetDocument(ContributorService.SEARCH_INDEX_NAME,
						ContributorService.SEARCH_INDEX_TYPE, "1");
				Assert.assertNotNull(r);
				Assert.assertEquals("v1", r.get("name"));
			}

			// case - insert noexisting object to existing index
			{
				Map<String, Object> entity = new HashMap<String, Object>();
				entity.put("name", "v2");
				tested.create("2", entity);
				Mockito.verify(tested.entityService).create("2", entity);
				Map<String, Object> r = indexGetDocument(ContributorService.SEARCH_INDEX_NAME,
						ContributorService.SEARCH_INDEX_TYPE, "2");
				Assert.assertNotNull(r);
				Assert.assertEquals("v2", r.get("name"));
				r = indexGetDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, "1");
				Assert.assertNotNull(r);
				Assert.assertEquals("v1", r.get("name"));
			}

			// case - update existing object
			{
				Map<String, Object> entity = new HashMap<String, Object>();
				entity.put("name", "v1_1");
				tested.create("1", entity);
				Mockito.verify(tested.entityService).create("1", entity);
				Map<String, Object> r = indexGetDocument(ContributorService.SEARCH_INDEX_NAME,
						ContributorService.SEARCH_INDEX_TYPE, "2");
				Assert.assertNotNull(r);
				Assert.assertEquals("v2", r.get("name"));
				r = indexGetDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, "1");
				Assert.assertNotNull(r);
				Assert.assertEquals("v1_1", r.get("name"));
			}
		} finally {
			indexDelete(ContributorService.SEARCH_INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	@Test
	public void update() {
		Client client = prepareESClientForUnitTest();
		ContributorService tested = getTested(client);
		try {

			// case - insert noexisting object to noexisting index
			{
				Mockito.reset(tested.entityService);
				Map<String, Object> entity = new HashMap<String, Object>();
				entity.put("name", "v1");

				tested.update("1", entity);
				Mockito.verify(tested.entityService).update("1", entity);
				Map<String, Object> r = indexGetDocument(ContributorService.SEARCH_INDEX_NAME,
						ContributorService.SEARCH_INDEX_TYPE, "1");
				Assert.assertNotNull(r);
				Assert.assertEquals("v1", r.get("name"));
			}

			// case - insert noexisting object to existing index
			{
				Mockito.reset(tested.entityService);
				Map<String, Object> entity = new HashMap<String, Object>();
				entity.put("name", "v2");
				tested.update("2", entity);
				Mockito.verify(tested.entityService).update("2", entity);
				Map<String, Object> r = indexGetDocument(ContributorService.SEARCH_INDEX_NAME,
						ContributorService.SEARCH_INDEX_TYPE, "2");
				Assert.assertNotNull(r);
				Assert.assertEquals("v2", r.get("name"));
				r = indexGetDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, "1");
				Assert.assertNotNull(r);
				Assert.assertEquals("v1", r.get("name"));
			}

			// case - update existing object
			{
				Mockito.reset(tested.entityService);
				Map<String, Object> entity = new HashMap<String, Object>();
				entity.put("name", "v1_1");
				tested.update("1", entity);
				Mockito.verify(tested.entityService).update("1", entity);
				Map<String, Object> r = indexGetDocument(ContributorService.SEARCH_INDEX_NAME,
						ContributorService.SEARCH_INDEX_TYPE, "2");
				Assert.assertNotNull(r);
				Assert.assertEquals("v2", r.get("name"));
				r = indexGetDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, "1");
				Assert.assertNotNull(r);
				Assert.assertEquals("v1_1", r.get("name"));
			}
		} finally {
			indexDelete(ContributorService.SEARCH_INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	@Test
	public void delete() throws InterruptedException {
		Client client = prepareESClientForUnitTest();
		ContributorService tested = getTested(client);
		try {

			// case - delete from noexisting index
			{
				Mockito.reset(tested.entityService);
				tested.delete("1");
				Mockito.verify(tested.entityService).delete("1");
			}

			indexDelete(ContributorService.SEARCH_INDEX_NAME);
			indexCreate(ContributorService.SEARCH_INDEX_NAME);
			// case - index exists but record not in it
			{
				Mockito.reset(tested.entityService);
				tested.delete("1");
				Mockito.verify(tested.entityService).delete("1");
			}

			indexInsertDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, "10",
					"{\"name\":\"test1\",\"idx\":\"1\"}");
			indexInsertDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, "20",
					"{\"name\":\"test2\",\"idx\":\"2\"}");
			indexInsertDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, "30",
					"{\"name\":\"test3\",\"idx\":\"3\"}");
			indexFlush(ContributorService.SEARCH_INDEX_NAME);
			// case - index exists and record deleted
			{
				Mockito.reset(tested.entityService);
				tested.delete("10");
				Mockito.verify(tested.entityService).delete("10");
				Assert.assertNull(indexGetDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE,
						"10"));
				Assert.assertNotNull(indexGetDocument(ContributorService.SEARCH_INDEX_NAME,
						ContributorService.SEARCH_INDEX_TYPE, "20"));
				Assert.assertNotNull(indexGetDocument(ContributorService.SEARCH_INDEX_NAME,
						ContributorService.SEARCH_INDEX_TYPE, "30"));

				Mockito.reset(tested.entityService);
				tested.delete("30");
				Mockito.verify(tested.entityService).delete("30");
				Assert.assertNull(indexGetDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE,
						"10"));
				Assert.assertNotNull(indexGetDocument(ContributorService.SEARCH_INDEX_NAME,
						ContributorService.SEARCH_INDEX_TYPE, "20"));
				Assert.assertNull(indexGetDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE,
						"30"));
			}

		} finally {
			indexDelete(ContributorService.SEARCH_INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	@Test
	public void search() throws Exception {
		Client client = prepareESClientForUnitTest();
		ContributorService tested = getTested(client);
		try {
			// case - search from noexisting index
			{
				SearchResponse sr = tested.search("test@test.org");
				Assert.assertNull(sr);
			}

			tested.init();
			Thread.sleep(100);
			// case - search from empty index
			{
				SearchResponse sr = tested.search("test@test.org");
				Assert.assertEquals(0, sr.hits().getTotalHits());
			}

			indexInsertDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, "10",
					"{\"name\":\"test1\",\"idx\":\"1\",\"email\":\"me@test.org\"}");
			indexInsertDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, "20",
					"{\"name\":\"test2\",\"idx\":\"2\",\"email\":\"test@test.org\"}");
			indexInsertDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, "30",
					"{\"name\":\"test3\",\"idx\":\"3\",\"email\":\"he@test.org\"}");
			indexFlush(ContributorService.SEARCH_INDEX_NAME);
			// case - search existing
			{
				SearchResponse sr = tested.search("test@test.org");
				Assert.assertEquals(1, sr.hits().getTotalHits());
				Assert.assertEquals("20", sr.hits().hits()[0].getId());

				sr = tested.search("me@test.org");
				Assert.assertEquals(1, sr.hits().getTotalHits());
				Assert.assertEquals("10", sr.hits().hits()[0].getId());
			}

		} finally {
			indexDelete(ContributorService.SEARCH_INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

}
