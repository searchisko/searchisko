/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.core.StreamingOutput;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.searchisko.api.rest.ESDataOnlyResponse;
import org.searchisko.api.testtools.ESRealClientTestBase;
import org.searchisko.persistence.service.EntityService;
import org.searchisko.persistence.service.ListRequest;

/**
 * Unit test for {@link ProjectService}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ProjectServiceTest extends ESRealClientTestBase {

	private ProjectService getTested(Client client) {
		if (client == null)
			client = Mockito.mock(Client.class);

		ProjectService ret = new ProjectService();
		ret.entityService = Mockito.mock(EntityService.class);
		ret.searchClientService = new SearchClientService();
		ret.searchClientService.log = Logger.getLogger("testlogger");
		ret.searchClientService.client = client;
		ret.log = Logger.getLogger("testlogger");
		return ret;
	}

	@Test
	public void getAll() {
		ProjectService tested = getTested(null);

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
	public void getAll_raw() {
		ProjectService tested = getTested(null);

		// case - value is returned
		List<Map<String, Object>> value = new ArrayList<Map<String, Object>>();
		Mockito.when(tested.entityService.getAll()).thenReturn(value);
		Assert.assertEquals(value, tested.getAll());

		// case - null is returned
		Mockito.reset(tested.entityService);
		Mockito.when(tested.entityService.getAll()).thenReturn(null);
		Assert.assertEquals(null, tested.getAll());
		Mockito.verify(tested.entityService).getAll();
		Mockito.verifyNoMoreInteractions(tested.entityService);

		// case - exception is passed
		Mockito.reset(tested.entityService);
		Mockito.when(tested.entityService.getAll()).thenThrow(new RuntimeException("testex"));
		try {
			tested.getAll();
			Assert.fail("RuntimeException expected");
		} catch (RuntimeException e) {
			// OK
		}
		Mockito.verify(tested.entityService).getAll();
		Mockito.verifyNoMoreInteractions(tested.entityService);
	}

	@Test
	public void get() {
		ProjectService tested = getTested(null);

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
		Client client = prepareESClientForUnitTest("ProjectServiceTest");
		ProjectService tested = getTested(client);
		try {

			// case - insert to noexisting index
			{
				Map<String, Object> entity = new HashMap<String, Object>();
				entity.put("name", "v1");
				Mockito.when(tested.entityService.create(entity)).thenReturn("1");

				String id = tested.create(entity);
				Assert.assertEquals("1", id);
				Map<String, Object> r = indexGetDocument(ProjectService.SEARCH_INDEX_NAME, ProjectService.SEARCH_INDEX_TYPE,
						"1");
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
				Map<String, Object> r = indexGetDocument(ProjectService.SEARCH_INDEX_NAME, ProjectService.SEARCH_INDEX_TYPE,
						"2");
				Assert.assertNotNull(r);
				Assert.assertEquals("v2", r.get("name"));
				r = indexGetDocument(ProjectService.SEARCH_INDEX_NAME, ProjectService.SEARCH_INDEX_TYPE, "1");
				Assert.assertNotNull(r);
				Assert.assertEquals("v1", r.get("name"));

			}

		} finally {
			indexDelete(ProjectService.SEARCH_INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	@Test
	public void create_id() {
		Client client = prepareESClientForUnitTest("ProjectServiceTest_2");
		ProjectService tested = getTested(client);
		try {

			// case - insert noexisting object to noexisting index
			{
				Map<String, Object> entity = new HashMap<String, Object>();
				entity.put("name", "v1");

				tested.create("1", entity);
				Mockito.verify(tested.entityService).create("1", entity);
				Map<String, Object> r = indexGetDocument(ProjectService.SEARCH_INDEX_NAME, ProjectService.SEARCH_INDEX_TYPE,
						"1");
				Assert.assertNotNull(r);
				Assert.assertEquals("v1", r.get("name"));
			}

			// case - insert noexisting object to existing index
			{
				Map<String, Object> entity = new HashMap<String, Object>();
				entity.put("name", "v2");
				tested.create("2", entity);
				Mockito.verify(tested.entityService).create("2", entity);
				Map<String, Object> r = indexGetDocument(ProjectService.SEARCH_INDEX_NAME, ProjectService.SEARCH_INDEX_TYPE,
						"2");
				Assert.assertNotNull(r);
				Assert.assertEquals("v2", r.get("name"));
				r = indexGetDocument(ProjectService.SEARCH_INDEX_NAME, ProjectService.SEARCH_INDEX_TYPE, "1");
				Assert.assertNotNull(r);
				Assert.assertEquals("v1", r.get("name"));
			}

			// case - update existing object
			{
				Map<String, Object> entity = new HashMap<String, Object>();
				entity.put("name", "v1_1");
				tested.create("1", entity);
				Mockito.verify(tested.entityService).create("1", entity);
				Map<String, Object> r = indexGetDocument(ProjectService.SEARCH_INDEX_NAME, ProjectService.SEARCH_INDEX_TYPE,
						"2");
				Assert.assertNotNull(r);
				Assert.assertEquals("v2", r.get("name"));
				r = indexGetDocument(ProjectService.SEARCH_INDEX_NAME, ProjectService.SEARCH_INDEX_TYPE, "1");
				Assert.assertNotNull(r);
				Assert.assertEquals("v1_1", r.get("name"));
			}
		} finally {
			indexDelete(ProjectService.SEARCH_INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	@Test
	public void update() {
		Client client = prepareESClientForUnitTest("ProjectServiceTest_3");
		ProjectService tested = getTested(client);
		try {

			// case - insert noexisting object to noexisting index
			{
				Mockito.reset(tested.entityService);
				Map<String, Object> entity = new HashMap<String, Object>();
				entity.put("name", "v1");

				tested.update("1", entity);
				Mockito.verify(tested.entityService).update("1", entity);
				Map<String, Object> r = indexGetDocument(ProjectService.SEARCH_INDEX_NAME, ProjectService.SEARCH_INDEX_TYPE,
						"1");
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
				Map<String, Object> r = indexGetDocument(ProjectService.SEARCH_INDEX_NAME, ProjectService.SEARCH_INDEX_TYPE,
						"2");
				Assert.assertNotNull(r);
				Assert.assertEquals("v2", r.get("name"));
				r = indexGetDocument(ProjectService.SEARCH_INDEX_NAME, ProjectService.SEARCH_INDEX_TYPE, "1");
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
				Map<String, Object> r = indexGetDocument(ProjectService.SEARCH_INDEX_NAME, ProjectService.SEARCH_INDEX_TYPE,
						"2");
				Assert.assertNotNull(r);
				Assert.assertEquals("v2", r.get("name"));
				r = indexGetDocument(ProjectService.SEARCH_INDEX_NAME, ProjectService.SEARCH_INDEX_TYPE, "1");
				Assert.assertNotNull(r);
				Assert.assertEquals("v1_1", r.get("name"));
			}
		} finally {
			indexDelete(ProjectService.SEARCH_INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	@Test
	public void delete() throws InterruptedException {
		Client client = prepareESClientForUnitTest("ProjectServiceTest_4");
		ProjectService tested = getTested(client);
		try {

			// case - delete from noexisting index
			{
				Mockito.reset(tested.entityService);
				tested.delete("1");
				Mockito.verify(tested.entityService).delete("1");
			}

			indexDelete(ProjectService.SEARCH_INDEX_NAME);
			indexCreate(ProjectService.SEARCH_INDEX_NAME);
			Thread.sleep(100);
			// case - index exists but record not in it
			{
				Mockito.reset(tested.entityService);
				tested.delete("1");
				Mockito.verify(tested.entityService).delete("1");
			}

			indexInsertDocument(ProjectService.SEARCH_INDEX_NAME, ProjectService.SEARCH_INDEX_TYPE, "10",
					"{\"name\":\"test1\",\"idx\":\"1\"}");
			indexInsertDocument(ProjectService.SEARCH_INDEX_NAME, ProjectService.SEARCH_INDEX_TYPE, "20",
					"{\"name\":\"test2\",\"idx\":\"2\"}");
			indexInsertDocument(ProjectService.SEARCH_INDEX_NAME, ProjectService.SEARCH_INDEX_TYPE, "30",
					"{\"name\":\"test3\",\"idx\":\"3\"}");
			indexFlushAndRefresh(ProjectService.SEARCH_INDEX_NAME);
			// case - index exists and record deleted
			{
				Mockito.reset(tested.entityService);
				tested.delete("10");
				Mockito.verify(tested.entityService).delete("10");
				Assert.assertNull(indexGetDocument(ProjectService.SEARCH_INDEX_NAME, ProjectService.SEARCH_INDEX_TYPE, "10"));
				Assert
						.assertNotNull(indexGetDocument(ProjectService.SEARCH_INDEX_NAME, ProjectService.SEARCH_INDEX_TYPE, "20"));
				Assert
						.assertNotNull(indexGetDocument(ProjectService.SEARCH_INDEX_NAME, ProjectService.SEARCH_INDEX_TYPE, "30"));

				Mockito.reset(tested.entityService);
				tested.delete("30");
				Mockito.verify(tested.entityService).delete("30");
				Assert.assertNull(indexGetDocument(ProjectService.SEARCH_INDEX_NAME, ProjectService.SEARCH_INDEX_TYPE, "10"));
				Assert
						.assertNotNull(indexGetDocument(ProjectService.SEARCH_INDEX_NAME, ProjectService.SEARCH_INDEX_TYPE, "20"));
				Assert.assertNull(indexGetDocument(ProjectService.SEARCH_INDEX_NAME, ProjectService.SEARCH_INDEX_TYPE, "30"));
			}

		} finally {
			indexDelete(ProjectService.SEARCH_INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	@Test
	public void findByCode() throws Exception {
		Client client = prepareESClientForUnitTest("ProjectServiceTest_5");
		ProjectService tested = getTested(client);
		try {
			// case - search from noexisting index
			{
				indexDelete(ProjectService.SEARCH_INDEX_NAME);
				SearchResponse sr = tested.findByCode("jbosstools");
				Assert.assertNull(sr);
			}

			// case - search from empty index
			indexCreate(ProjectService.SEARCH_INDEX_NAME);
			{
				SearchResponse sr = tested.findByCode("jbosstools");
				Assert.assertEquals(0, sr.getHits().getTotalHits());
			}

			indexInsertDocument(ProjectService.SEARCH_INDEX_NAME, ProjectService.SEARCH_INDEX_TYPE, "10",
					"{\"code\":\"jbosstools\"}");
			indexInsertDocument(ProjectService.SEARCH_INDEX_NAME, ProjectService.SEARCH_INDEX_TYPE, "20",
					"{\"code\":\"jbossas\"}");
			indexInsertDocument(ProjectService.SEARCH_INDEX_NAME, ProjectService.SEARCH_INDEX_TYPE, "30",
					"{\"code\":\"aerogear\"}");
			indexFlushAndRefresh(ProjectService.SEARCH_INDEX_NAME);
			// case - search existing
			{
				SearchResponse sr = tested.findByCode("jbossas");
				Assert.assertEquals(1, sr.getHits().getTotalHits());
				Assert.assertEquals("20", sr.getHits().getHits()[0].getId());

				sr = tested.findByCode("jbosstools");
				Assert.assertEquals(1, sr.getHits().getTotalHits());
				Assert.assertEquals("10", sr.getHits().getHits()[0].getId());

				sr = tested.findByCode("spring");
				Assert.assertEquals(0, sr.getHits().getTotalHits());
			}

		} finally {
			indexDelete(ProjectService.SEARCH_INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	private static final String CODE_NAME_1 = "code_type1";
	private static final String CODE_NAME_2 = "code_type2";

	@Test
	public void findByTypeSpecificCode() throws InterruptedException {
		Client client = prepareESClientForUnitTest("ProjectServiceTest_6");
		ProjectService tested = getTested(client);
		try {
			// case - search from noexisting index
			indexDelete(ProjectService.SEARCH_INDEX_NAME);
			{
				SearchResponse sr = tested.findByTypeSpecificCode(CODE_NAME_1, "test");
				Assert.assertNull(sr);
			}

			indexCreate(ProjectService.SEARCH_INDEX_NAME);
			// case - search from empty index
			{
				SearchResponse sr = tested.findByTypeSpecificCode(CODE_NAME_2, "test");
				Assert.assertEquals(0, sr.getHits().getTotalHits());
			}

			indexInsertDocument(ProjectService.SEARCH_INDEX_NAME, ProjectService.SEARCH_INDEX_TYPE, "10",
					"{\"code\":\"test1\""
							+ ", \"type_specific_code\" : {\"code_type1\":\"test\",\"code_type2\":[\"ct2_1_1\",\"ct2_1_2\"]}" + "}");
			indexInsertDocument(ProjectService.SEARCH_INDEX_NAME, ProjectService.SEARCH_INDEX_TYPE, "20",
					"{\"code\":\"test2\""
							+ ", \"type_specific_code\" : {\"code_type1\":\"ct1_2\",\"code_type2\":[\"ct2_2_1\",\"test\"]}" + "}");
			indexInsertDocument(ProjectService.SEARCH_INDEX_NAME, ProjectService.SEARCH_INDEX_TYPE, "30",
					"{\"code\":\"test3\""
							+ ", \"type_specific_code\" : {\"code_type1\":\"ct1_3\",\"code_type2\":[\"ct2_3_1\",\"test_3_2\"]}" + "}");
			indexFlushAndRefresh(ProjectService.SEARCH_INDEX_NAME);
			// case - search existing
			{
				SearchResponse sr = tested.findByTypeSpecificCode(CODE_NAME_1, "test");
				Assert.assertEquals(1, sr.getHits().getTotalHits());
				Assert.assertEquals("10", sr.getHits().getHits()[0].getId());

				sr = tested.findByTypeSpecificCode(CODE_NAME_2, "test");
				Assert.assertEquals(1, sr.getHits().getTotalHits());
				Assert.assertEquals("20", sr.getHits().getHits()[0].getId());

				sr = tested.findByTypeSpecificCode(CODE_NAME_2, "te");
				Assert.assertEquals(0, sr.getHits().getTotalHits());
			}

		} finally {
			indexDelete(ProjectService.SEARCH_INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	@Test
	public void listRequestInit() {
		ProjectService tested = getTested(null);
		ListRequest expected = Mockito.mock(ListRequest.class);
		Mockito.when(tested.entityService.listRequestInit()).thenReturn(expected);
		Assert.assertEquals(expected, tested.listRequestInit());
		Mockito.verify(tested.entityService).listRequestInit();
		Mockito.verifyNoMoreInteractions(tested.entityService);
	}

	@Test
	public void listRequestNext() {
		ProjectService tested = getTested(null);
		ListRequest expected = Mockito.mock(ListRequest.class);
		ListRequest prev = Mockito.mock(ListRequest.class);
		Mockito.when(tested.entityService.listRequestNext(prev)).thenReturn(expected);
		Assert.assertEquals(expected, tested.listRequestNext(prev));
		Mockito.verify(tested.entityService).listRequestNext(prev);
		Mockito.verifyNoMoreInteractions(tested.entityService);
	}

	@Test
	public void prepareBulkRequest_updateSearchIndexBrb() {
		Client client = prepareESClientForUnitTest("ProjectServiceTest_7");
		ProjectService tested = getTested(client);
		try {

			BulkRequestBuilder brb = tested.prepareBulkRequest();

			tested.updateSearchIndex(brb, "10", prepareEntity("10"));
			tested.updateSearchIndex(brb, "20", prepareEntity("20"));
			tested.updateSearchIndex(brb, "30", prepareEntity("30"));

			brb.execute().actionGet();

			indexFlushAndRefresh(ProjectService.SEARCH_INDEX_NAME);

			Assert.assertNotNull(indexGetDocument(ProjectService.SEARCH_INDEX_NAME, ProjectService.SEARCH_INDEX_TYPE, "10"));
			Assert.assertNotNull(indexGetDocument(ProjectService.SEARCH_INDEX_NAME, ProjectService.SEARCH_INDEX_TYPE, "20"));
			Assert.assertNotNull(indexGetDocument(ProjectService.SEARCH_INDEX_NAME, ProjectService.SEARCH_INDEX_TYPE, "30"));

		} finally {
			indexDelete(ProjectService.SEARCH_INDEX_NAME);
			finalizeESClientForUnitTest();
		}

	}

	private Map<String, Object> prepareEntity(String code) {
		Map<String, Object> entity = new HashMap<String, Object>();
		entity.put("code", code);
		return entity;
	}

	@Test
	public void deleteOldFromSearchIndex() throws InterruptedException {
		Client client = prepareESClientForUnitTest("ProjectServiceTest_8");
		ProjectService tested = getTested(client);
		try {

			indexCreate(ProjectService.SEARCH_INDEX_NAME);
			indexMappingCreate(ProjectService.SEARCH_INDEX_NAME, ProjectService.SEARCH_INDEX_TYPE, "{ \""
					+ ProjectService.SEARCH_INDEX_TYPE + "\" : {\"_timestamp\" : { \"enabled\" : true }}}");

			tested.create("10", prepareEntity("10"));
			tested.create("20", prepareEntity("20"));
			indexFlushAndRefresh(ProjectService.SEARCH_INDEX_NAME);
			Date timestamp = new Date();
			tested.create("30", prepareEntity("30"));
			tested.create("40", prepareEntity("40"));

			tested.deleteOldFromSearchIndex(timestamp);
			indexFlushAndRefresh(ProjectService.SEARCH_INDEX_NAME);

			Assert.assertNull(indexGetDocument(ProjectService.SEARCH_INDEX_NAME, ProjectService.SEARCH_INDEX_TYPE, "10"));
			Assert.assertNull(indexGetDocument(ProjectService.SEARCH_INDEX_NAME, ProjectService.SEARCH_INDEX_TYPE, "20"));

			Assert.assertNotNull(indexGetDocument(ProjectService.SEARCH_INDEX_NAME, ProjectService.SEARCH_INDEX_TYPE, "30"));
			Assert.assertNotNull(indexGetDocument(ProjectService.SEARCH_INDEX_NAME, ProjectService.SEARCH_INDEX_TYPE, "40"));

		} finally {
			indexDelete(ProjectService.SEARCH_INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

}
