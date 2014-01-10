/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.core.StreamingOutput;

import junit.framework.Assert;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.junit.Test;
import org.mockito.Mockito;
import org.searchisko.api.rest.ESDataOnlyResponse;
import org.searchisko.api.testtools.ESRealClientTestBase;
import org.searchisko.api.testtools.TestUtils;
import org.searchisko.persistence.service.EntityService;

/**
 * Unit test for {@link ContributorService}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ContributorServiceTest extends ESRealClientTestBase {

	private static final String EMAIL_3 = "me@test";
	private static final String EMAIL_2 = "me@test.org";
	private static final String EMAIL_1 = "test@test.org";
	private static final String CODE_3 = "jan doe <test@test.org>";
	private static final String CODE_2 = "john doe 2 <test2@test.org>";
	private static final String CODE_1 = "john doe <test@test.org>";

	@Test
	public void createContributorId() {

		Assert.assertEquals("full name <email>", ContributorService.createContributorId("full name", "email"));
		// trim
		Assert.assertEquals("full Name <email>", ContributorService.createContributorId("full Name ", " email "));
		// email to lower case
		Assert.assertEquals("full Name <email>", ContributorService.createContributorId("full Name ", " eMail "));
		try {
			ContributorService.createContributorId(null, "email");
			Assert.fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
		}

		try {
			ContributorService.createContributorId("", "email");
			Assert.fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
		}

		try {
			ContributorService.createContributorId("  ", "email");
			Assert.fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
		}

		try {
			ContributorService.createContributorId("full name", null);
			Assert.fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
		}

		try {
			ContributorService.createContributorId("full name", "");
			Assert.fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
		}

		try {
			ContributorService.createContributorId("full name", "  ");
			Assert.fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
		}

	}

	@Test
	public void extractContributorName() {
		Assert.assertNull(ContributorService.extractContributorName(null));
		Assert.assertNull(ContributorService.extractContributorName(""));

		// no email present
		Assert.assertEquals("John Doe", ContributorService.extractContributorName("John Doe"));
		Assert.assertEquals("John Doe", ContributorService.extractContributorName(" John Doe "));
		Assert.assertEquals("John > Doe", ContributorService.extractContributorName("John > Doe"));
		Assert.assertEquals("John < Doe", ContributorService.extractContributorName("John < Doe"));
		Assert.assertEquals("John Doe <", ContributorService.extractContributorName("John Doe <"));
		Assert.assertEquals("John Doe >", ContributorService.extractContributorName("John Doe >"));
		Assert.assertEquals("John >< Doe", ContributorService.extractContributorName("John >< Doe"));

		// remove email
		Assert.assertEquals("John Doe", ContributorService.extractContributorName("John Doe<john@doe.org>"));
		Assert.assertEquals("John Doe", ContributorService.extractContributorName("John Doe <john@doe.org>"));
		Assert.assertEquals("John > Doe", ContributorService.extractContributorName("John > Doe <john@doe.org>"));
		Assert.assertEquals("John Doe", ContributorService.extractContributorName("John Doe<john@doe.org> "));
		Assert.assertEquals("John Doe", ContributorService.extractContributorName("John Doe <john@doe.org> "));
		Assert.assertEquals("John Doe", ContributorService.extractContributorName("John Doe <> "));
		Assert.assertEquals("John Doe", ContributorService.extractContributorName("John Doe<> "));
		Assert.assertNull(ContributorService.extractContributorName("<john@doe.org>"));
		Assert.assertNull(ContributorService.extractContributorName(" <john@doe.org>"));
	}

	private ContributorService getTested(Client client) {
		if (client == null)
			client = Mockito.mock(Client.class);

		ContributorService ret = new ContributorService();
		ret.entityService = Mockito.mock(EntityService.class);
		ret.searchClientService = new SearchClientService();
		ret.searchClientService.client = client;
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
	public void getAll_raw() {
		ContributorService tested = getTested(null);

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
			indexDelete(ContributorService.SEARCH_INDEX_NAME);
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
			indexDelete(ContributorService.SEARCH_INDEX_NAME);
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
			indexDelete(ContributorService.SEARCH_INDEX_NAME);
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

			indexDelete(ContributorService.SEARCH_INDEX_NAME);
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
			indexFlushAndRefresh(ContributorService.SEARCH_INDEX_NAME);
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
	public void findByCode_findOneByCode() throws Exception {
		Client client = prepareESClientForUnitTest();
		ContributorService tested = getTested(client);
		try {
			// case - search from noexisting index
			{
				indexDelete(ContributorService.SEARCH_INDEX_NAME);
				Assert.assertNull(tested.findByCode(CODE_1));
				Assert.assertNull(tested.findOneByCode(CODE_1));
			}

			tested.init();
			Thread.sleep(100);
			// case - search from empty index
			{
				SearchResponse sr = tested.findByCode(CODE_1);
				Assert.assertEquals(0, sr.getHits().getTotalHits());
				Assert.assertNull(tested.findOneByCode(CODE_1));
			}

			indexInsertDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, "10",
					"{\"code\":\"" + CODE_2 + "\",\"email\":\"test2@test.org\"}");
			indexInsertDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, "20",
					"{\"code\":\"" + CODE_1 + "\",\"email\":\"test@test.org\"}");
			indexInsertDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, "30",
					"{\"code\":\"paul doe <t@te.org>\",\"email\":\"te@te.org\"}");
			indexFlushAndRefresh(ContributorService.SEARCH_INDEX_NAME);
			// case - search existing
			{
				SearchResponse sr = tested.findByCode(CODE_1);
				Assert.assertEquals(1, sr.getHits().getTotalHits());
				Assert.assertEquals("20", sr.getHits().getHits()[0].getId());

				sr = tested.findByCode(CODE_2);
				Assert.assertEquals(1, sr.getHits().getTotalHits());
				Assert.assertEquals("10", sr.getHits().getHits()[0].getId());
				Assert.assertEquals("10", tested.findOneByCode(CODE_2).getId());

				sr = tested.findByCode(CODE_3);
				Assert.assertEquals(0, sr.getHits().getTotalHits());
				Assert.assertNull(tested.findOneByCode(CODE_3));
			}

		} finally {
			indexDelete(ContributorService.SEARCH_INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	@Test
	public void findByEmail_findOneByEmail() throws Exception {
		Client client = prepareESClientForUnitTest();
		ContributorService tested = getTested(client);
		try {
			// case - search from noexisting index
			{
				indexDelete(ContributorService.SEARCH_INDEX_NAME);
				Assert.assertNull(tested.findByEmail(EMAIL_1));
				Assert.assertNull(tested.findOneByEmail(EMAIL_1, null));
			}

			tested.init();
			Thread.sleep(100);
			// case - search from empty index
			{
				SearchResponse sr = tested.findByEmail(EMAIL_1);
				Assert.assertEquals(0, sr.getHits().getTotalHits());
				Assert.assertNull(tested.findOneByEmail(EMAIL_1, null));
			}

			indexInsertDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, "10",
					"{\"code\":\"test1\",\"email\":\"" + EMAIL_2 + "\"}");
			indexInsertDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, "20",
					"{\"code\":\"test2\",\"email\":\"" + EMAIL_1 + "\"}");
			indexInsertDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, "30",
					"{\"code\":\"test3\",\"email\":\"he@test.org\"}");
			indexFlushAndRefresh(ContributorService.SEARCH_INDEX_NAME);
			// case - search existing
			{
				SearchResponse sr = tested.findByEmail(EMAIL_1);
				Assert.assertEquals(1, sr.getHits().getTotalHits());
				Assert.assertEquals("20", sr.getHits().getHits()[0].getId());
				List<String> emails = new ArrayList<>();
				emails.add(EMAIL_2);
				Assert.assertEquals("20", tested.findOneByEmail(EMAIL_1, null).getId());

				sr = tested.findByEmail(EMAIL_2);
				Assert.assertEquals(1, sr.getHits().getTotalHits());
				Assert.assertEquals("10", sr.getHits().getHits()[0].getId());
				emails.add(EMAIL_2);
				Assert.assertEquals("10", tested.findOneByEmail(EMAIL_3, emails).getId());

				sr = tested.findByEmail(EMAIL_3);
				Assert.assertEquals(0, sr.getHits().getTotalHits());
			}

		} finally {
			indexDelete(ContributorService.SEARCH_INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	private static final String CODE_NAME_1 = "code_type1";
	private static final String CODE_NAME_2 = "code_type2";

	@Test
	public void findByTypeSpecificCode() throws InterruptedException {
		Client client = prepareESClientForUnitTest();
		ContributorService tested = getTested(client);
		try {
			// case - search from noexisting index
			indexDelete(ContributorService.SEARCH_INDEX_NAME);
			{
				Assert.assertNull(tested.findByTypeSpecificCode(CODE_NAME_1, "test"));
			}

			tested.init();
			Thread.sleep(100);
			// case - search from empty index
			{
				SearchResponse sr = tested.findByTypeSpecificCode(CODE_NAME_2, "test");
				Assert.assertEquals(0, sr.getHits().getTotalHits());
			}

			indexInsertDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, "10",
					"{\"code\":\"test1\",\"email\":\"me@test.org\""
							+ ", \"type_specific_code\" : {\"code_type1\":\"test\",\"code_type2\":[\"ct2_1_1\",\"ct2_1_2\"]}" + "}");
			indexInsertDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, "20",
					"{\"code\":\"test2\",\"email\":\"test@test.org\""
							+ ", \"type_specific_code\" : {\"code_type1\":\"ct1_2\",\"code_type2\":[\"ct2_2_1\",\"test\"]}" + "}");
			indexInsertDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, "30",
					"{\"code\":\"test3\",\"email\":\"he@test.org\""
							+ ", \"type_specific_code\" : {\"code_type1\":\"ct1_3\",\"code_type2\":[\"ct2_3_1\",\"test_3_2\"]}" + "}");
			indexFlushAndRefresh(ContributorService.SEARCH_INDEX_NAME);
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
			indexDelete(ContributorService.SEARCH_INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	@Test
	public void findOneByTypeSpecificCode() throws InterruptedException {
		Client client = prepareESClientForUnitTest();
		ContributorService tested = getTested(client);
		try {
			// case - search from noexisting index
			indexDelete(ContributorService.SEARCH_INDEX_NAME);
			{
				Assert.assertNull(tested.findOneByTypeSpecificCode(CODE_NAME_1, "test", null));
			}

			tested.init();
			Thread.sleep(100);
			// case - search from empty index
			{
				Assert.assertNull(tested.findOneByTypeSpecificCode(CODE_NAME_2, "test", "test1"));
			}

			indexInsertDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, "10",
					"{\"code\":\"test1\",\"email\":\"me@test.org\""
							+ ", \"type_specific_code\" : {\"code_type1\":\"test\",\"code_type2\":[\"ct2_1_1\",\"ct2_1_2\"]}" + "}");
			indexInsertDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, "20",
					"{\"code\":\"test2\",\"email\":\"test@test.org\""
							+ ", \"type_specific_code\" : {\"code_type1\":\"ct1_2\",\"code_type2\":[\"ct2_2_1\",\"test\"]}" + "}");
			indexInsertDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, "30",
					"{\"code\":\"test3\",\"email\":\"he@test.org\""
							+ ", \"type_specific_code\" : {\"code_type1\":\"ct1_3\",\"code_type2\":[\"ct2_3_1\",\"test_3_2\"]}" + "}");
			indexFlushAndRefresh(ContributorService.SEARCH_INDEX_NAME);
			// case - search existing
			{
				Assert.assertEquals("10", tested.findOneByTypeSpecificCode(CODE_NAME_1, "test", null).getId());
				Assert.assertEquals("10", tested.findOneByTypeSpecificCode(CODE_NAME_1, "test", "test1").getId());
				Assert.assertEquals("10", tested.findOneByTypeSpecificCode(CODE_NAME_1, "test", "test2").getId());

				Assert.assertEquals("20", tested.findOneByTypeSpecificCode(CODE_NAME_2, "test", null).getId());

				Assert.assertNull(tested.findOneByTypeSpecificCode(CODE_NAME_2, "te", "test"));
			}

			// case - more contributors for same type specific code code, so we select one by prefered code
			indexInsertDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, "40",
					"{\"code\":\"test4\",\"email\":\"me@test.org\""
							+ ", \"type_specific_code\" : {\"code_type1\":\"test\",\"code_type2\":[\"ct2_1_1\",\"ct2_1_2\"]}" + "}");
			indexFlushAndRefresh(ContributorService.SEARCH_INDEX_NAME);
			{
				// first two asserts removed because it is unclear which one will be selected if no prefered code matches
				// Assert.assertEquals("10", tested.findOneByTypeSpecificCode(CODE_NAME_1, "test", null).getId());
				// Assert.assertEquals("10", tested.findOneByTypeSpecificCode(CODE_NAME_1, "test", "unknown").getId());
				Assert.assertEquals("10", tested.findOneByTypeSpecificCode(CODE_NAME_1, "test", "test1").getId());
				Assert.assertEquals("40", tested.findOneByTypeSpecificCode(CODE_NAME_1, "test", "test4").getId());
			}

		} finally {
			indexDelete(ContributorService.SEARCH_INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void mergeContributorData() {
		ContributorService tested = getTested(null);
		Map<String, Object> source = new HashMap<>();
		Map<String, Object> target = new HashMap<>();

		source.put(ContributorService.FIELD_CODE, "sourcecode");
		source.put(ContributorService.FIELD_EMAIL, TestUtils.createListOfStrings(EMAIL_1, EMAIL_2));
		Map<String, List<String>> sourceTSC = new HashMap<>();
		sourceTSC.put(CODE_NAME_1, TestUtils.createListOfStrings("c11_t", "c11"));
		sourceTSC.put(CODE_NAME_2, TestUtils.createListOfStrings("c21"));
		source.put(ContributorService.FIELD_TYPE_SPECIFIC_CODE, sourceTSC);

		target.put(ContributorService.FIELD_CODE, "targetcode");
		target.put(ContributorService.FIELD_EMAIL, TestUtils.createListOfStrings(EMAIL_3, EMAIL_2));
		Map<String, List<String>> targetTSC = new HashMap<>();
		targetTSC.put(CODE_NAME_1, TestUtils.createListOfStrings("c11"));
		target.put(ContributorService.FIELD_TYPE_SPECIFIC_CODE, targetTSC);

		tested.mergeContributorData(target, source);

		Assert.assertEquals("sourcecode", ContributorService.getContributorCode(source));
		Assert.assertEquals("targetcode", ContributorService.getContributorCode(target));

		Assert.assertTrue(target.get(ContributorService.FIELD_EMAIL) instanceof List);
		List l = (List) target.get(ContributorService.FIELD_EMAIL);
		Assert.assertEquals(3, l.size());
		Assert.assertTrue(l.contains(EMAIL_1));
		Assert.assertTrue(l.contains(EMAIL_2));
		Assert.assertTrue(l.contains(EMAIL_3));
		Assert.assertTrue(target.get(ContributorService.FIELD_TYPE_SPECIFIC_CODE) instanceof Map);
		Map<String, List<String>> m = (Map<String, List<String>>) target.get(ContributorService.FIELD_TYPE_SPECIFIC_CODE);
		List l1 = m.get(CODE_NAME_1);
		Assert.assertEquals(2, l1.size());
		Assert.assertTrue(l1.contains("c11"));
		Assert.assertTrue(l1.contains("c11_t"));
		List l2 = m.get(CODE_NAME_2);
		Assert.assertEquals(1, l2.size());
		Assert.assertTrue(l2.contains("c21"));
	}

	@Test
	public void getContributorCode() {

		try {
			ContributorService.getContributorCode(null);
			Assert.fail("NullPointerException expected");
		} catch (NullPointerException e) {

		}

		Map<String, Object> source = new HashMap<>();
		Assert.assertNull(ContributorService.getContributorCode(source));

		source.put(ContributorService.FIELD_CODE, "mycode");
		Assert.assertEquals("mycode", ContributorService.getContributorCode(source));
	}

	@Test
	public void createOrUpdateFromProfile() {
		// TODO CONTRIBUTOR_PROFILE unit test for ContributorService#createOrUpdateFromProfile
	}

}
