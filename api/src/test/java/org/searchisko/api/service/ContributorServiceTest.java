/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.ejb.ObjectNotFoundException;
import javax.enterprise.event.Event;
import javax.ws.rs.core.StreamingOutput;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.search.SearchHit;
import org.hamcrest.CustomMatcher;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.searchisko.api.events.ContributorCodeChangedEvent;
import org.searchisko.api.events.ContributorCreatedEvent;
import org.searchisko.api.events.ContributorDeletedEvent;
import org.searchisko.api.events.ContributorMergedEvent;
import org.searchisko.api.events.ContributorUpdatedEvent;
import org.searchisko.api.reindexer.ReindexingTaskTypes;
import org.searchisko.api.rest.ESDataOnlyResponse;
import org.searchisko.api.rest.exception.BadFieldException;
import org.searchisko.api.rest.exception.RequiredFieldException;
import org.searchisko.api.tasker.TaskManager;
import org.searchisko.api.testtools.ESRealClientTestBase;
import org.searchisko.api.testtools.TestUtils;
import org.searchisko.api.util.SearchUtils;
import org.searchisko.contribprofile.model.ContributorProfile;
import org.searchisko.persistence.service.EntityService;
import org.searchisko.persistence.service.ListRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link ContributorService}.<br>
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ContributorServiceTest extends ESRealClientTestBase {

	private static final String ID_40 = "40";
	private static final String ID_30 = "30";
	private static final String ID_0 = "0";
	private static final String ID_20 = "20";
	private static final String ID_10 = "10";
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

	@SuppressWarnings("unchecked")
	private ContributorService getTested(Client client) {
		if (client == null)
			client = Mockito.mock(Client.class);

		ContributorService ret = new ContributorService();
		ret.entityService = Mockito.mock(EntityService.class);
		ret.taskService = Mockito.mock(TaskService.class);
		TaskManager tm = Mockito.mock(TaskManager.class);
		Mockito.when(ret.taskService.getTaskManager()).thenReturn(tm);
		ret.searchClientService = new SearchClientService();
		ret.searchClientService.log = Logger.getLogger("testlogger");
		ret.searchClientService.client = client;
		ret.eventContributorCodeChanged = Mockito.mock(Event.class);
		ret.eventContributorMerged = Mockito.mock(Event.class);
		ret.eventCreate = Mockito.mock(Event.class);
		ret.eventUpdate = Mockito.mock(Event.class);
		ret.eventDelete = Mockito.mock(Event.class);
		ret.log = Logger.getLogger("testlogger");
		return ret;
	}

	protected void resetAllMocks(ContributorService tested) {
		Mockito.reset(tested.entityService, tested.taskService.getTaskManager(), tested.eventContributorMerged,
				tested.eventCreate, tested.eventDelete, tested.eventUpdate, tested.eventContributorCodeChanged);
	}

	protected void verifyNoMoreEventsFired(ContributorService tested) {
		Mockito.verifyNoMoreInteractions(tested.eventContributorCodeChanged);
		Mockito.verifyNoMoreInteractions(tested.eventContributorMerged);
		Mockito.verifyNoMoreInteractions(tested.eventDelete);
		Mockito.verifyNoMoreInteractions(tested.eventUpdate);
		Mockito.verifyNoMoreInteractions(tested.eventCreate);
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
		when(tested.entityService.get(ID_10)).thenReturn(value);
		Assert.assertEquals(value, tested.get(ID_10));

		// case - null is returned
		Mockito.reset(tested.entityService);
		when(tested.entityService.get(ID_10)).thenReturn(null);
		assertEquals(null, tested.get(ID_10));
		verify(tested.entityService).get(ID_10);
		verifyNoMoreInteractions(tested.entityService);

		// case - exception is passed
		reset(tested.entityService);
		when(tested.entityService.get(ID_10)).thenThrow(new RuntimeException("testex"));
		try {
			tested.get(ID_10);
			fail("RuntimeException expected");
		} catch (RuntimeException e) {
			// OK
		}
		verify(tested.entityService).get(ID_10);
		verifyNoMoreInteractions(tested.entityService);
	}

	@Test(expected = RequiredFieldException.class)
	public void create_noid_codeRequiredValidation() {
		ContributorService tested = getTested(null);

		Map<String, Object> entity = new HashMap<String, Object>();
		entity.put("name", "v1");
		try {
			tested.create(entity);
		} finally {
			verifyZeroInteractions(tested.eventUpdate);
			verifyZeroInteractions(tested.eventCreate);
		}
	}

	@Test(expected = BadFieldException.class)
	public void create_noid_codeDuplicityValidation() throws InterruptedException {
		Client client = prepareESClientForUnitTest("ContributorServiceTest");
		ContributorService tested = getTested(client);
		try {

			indexDelete(ContributorService.SEARCH_INDEX_NAME);
			tested.init();
			Thread.sleep(100);

			Map<String, Object> entity = new HashMap<String, Object>();
			entity.put(ContributorService.FIELD_CODE, CODE_1);
			when(tested.entityService.get("1")).thenReturn(null);
			tested.create("1", entity);
			tested.searchClientService.performIndexFlushAndRefreshBlocking(ContributorService.SEARCH_INDEX_NAME);
			tested.create(entity);
		} finally {
			verifyZeroInteractions(tested.eventUpdate);
			verify(tested.eventCreate, times(1)).fire(prepareContributorCreatedEventMatcher("1", CODE_1));
			indexDelete(ContributorService.SEARCH_INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	@Test
	public void create_noid() throws InterruptedException {
		Client client = prepareESClientForUnitTest("ContributorServiceTest_2");
		ContributorService tested = getTested(client);
		try {

			indexDelete(ContributorService.SEARCH_INDEX_NAME);
			tested.init();
			Thread.sleep(100);

			{
				Map<String, Object> entity = new HashMap<String, Object>();
				entity.put("name", "v1");
				entity.put(ContributorService.FIELD_CODE, CODE_1);
				when(tested.entityService.create(entity)).thenReturn("1");

				String id = tested.create(entity);
				assertEquals("1", id);
				Map<String, Object> r = indexGetDocument(ContributorService.SEARCH_INDEX_NAME,
						ContributorService.SEARCH_INDEX_TYPE, "1");
				assertNotNull(r);
				assertEquals("v1", r.get("name"));
				verify(tested.eventCreate).fire(prepareContributorCreatedEventMatcher(id, CODE_1));
				verifyZeroInteractions(tested.eventUpdate);
			}
			tested.searchClientService.performIndexFlushAndRefreshBlocking(ContributorService.SEARCH_INDEX_NAME);

			{
				reset(tested.entityService, tested.eventCreate, tested.eventUpdate);
				reset(tested.entityService);
				Map<String, Object> entity = new HashMap<String, Object>();
				entity.put("name", "v2");
				entity.put(ContributorService.FIELD_CODE, CODE_2);
				when(tested.entityService.create(entity)).thenReturn("2");

				String id = tested.create(entity);
				assertEquals("2", id);
				Map<String, Object> r = indexGetDocument(ContributorService.SEARCH_INDEX_NAME,
						ContributorService.SEARCH_INDEX_TYPE, "2");
				assertNotNull(r);
				assertEquals("v2", r.get("name"));
				r = indexGetDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, "1");
				assertNotNull(r);
				assertEquals("v1", r.get("name"));
				verify(tested.eventCreate).fire(prepareContributorCreatedEventMatcher(id, CODE_2));
				verifyZeroInteractions(tested.eventUpdate);
			}

		} finally {
			indexDelete(ContributorService.SEARCH_INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	@Test(expected = RequiredFieldException.class)
	public void create_id_codeRequiredValidation() {
		ContributorService tested = getTested(null);

		Map<String, Object> entity = new HashMap<String, Object>();
		entity.put("name", "v1");
		tested.create("1", entity);
	}

	@Test(expected = BadFieldException.class)
	public void create_id_codeChangeValidation() throws InterruptedException {
		Client client = prepareESClientForUnitTest("ContributorServiceTest_3");
		ContributorService tested = getTested(client);
		try {

			indexDelete(ContributorService.SEARCH_INDEX_NAME);
			tested.init();
			Thread.sleep(100);

			Map<String, Object> entity = new HashMap<String, Object>();
			entity.put(ContributorService.FIELD_CODE, "code_1");
			Map<String, Object> oldEntity = new HashMap<>();
			oldEntity.put(ContributorService.FIELD_CODE, "code_2");
			Mockito.when(tested.entityService.get("1")).thenReturn(oldEntity);

			tested.create("1", entity);
		} finally {
			indexDelete(ContributorService.SEARCH_INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	@Test(expected = BadFieldException.class)
	public void create_id_codeDuplicityValidation() throws InterruptedException {
		Client client = prepareESClientForUnitTest("ContributorServiceTest_4");
		ContributorService tested = getTested(client);
		try {

			indexDelete(ContributorService.SEARCH_INDEX_NAME);
			tested.init();
			Thread.sleep(100);

			Map<String, Object> entity = new HashMap<String, Object>();
			entity.put(ContributorService.FIELD_CODE, "code_1");
			tested.create("1", entity);
			tested.searchClientService.performIndexFlushAndRefreshBlocking(ContributorService.SEARCH_INDEX_NAME);
			tested.create("2", entity);
		} finally {
			indexDelete(ContributorService.SEARCH_INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	@Test
	public void create_id() throws InterruptedException {
		Client client = prepareESClientForUnitTest("ContributorServiceTest_5");
		ContributorService tested = getTested(client);
		try {

			indexDelete(ContributorService.SEARCH_INDEX_NAME);
			tested.init();
			Thread.sleep(100);

			// case - insert new objects
			{
				Map<String, Object> entity = new HashMap<String, Object>();
				entity.put("name", "v1");
				entity.put(ContributorService.FIELD_CODE, CODE_1);
				when(tested.entityService.get("1")).thenReturn(null);

				tested.create("1", entity);
				Mockito.verify(tested.entityService).create("1", entity);
				Map<String, Object> r = indexGetDocument(ContributorService.SEARCH_INDEX_NAME,
						ContributorService.SEARCH_INDEX_TYPE, "1");
				assertNotNull(r);
				assertEquals("v1", r.get("name"));
				verify(tested.eventCreate).fire(prepareContributorCreatedEventMatcher("1", CODE_1));
				verifyZeroInteractions(tested.eventUpdate);
			}
			tested.searchClientService.performIndexFlushAndRefreshBlocking(ContributorService.SEARCH_INDEX_NAME);

			{
				reset(tested.entityService, tested.eventCreate, tested.eventUpdate);
				when(tested.entityService.get("2")).thenReturn(null);
				Map<String, Object> entity = new HashMap<String, Object>();
				entity.put("name", "v2");
				entity.put(ContributorService.FIELD_CODE, CODE_2);
				tested.create("2", entity);
				verify(tested.entityService).create("2", entity);
				Map<String, Object> r = indexGetDocument(ContributorService.SEARCH_INDEX_NAME,
						ContributorService.SEARCH_INDEX_TYPE, "2");
				assertNotNull(r);
				assertEquals("v2", r.get("name"));
				r = indexGetDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, "1");
				assertNotNull(r);
				assertEquals("v1", r.get("name"));
				verify(tested.eventCreate).fire(prepareContributorCreatedEventMatcher("2", CODE_2));
				verifyZeroInteractions(tested.eventUpdate);
			}
			tested.searchClientService.performIndexFlushAndRefreshBlocking(ContributorService.SEARCH_INDEX_NAME);

			// case - update existing object
			{
				reset(tested.entityService, tested.eventCreate, tested.eventUpdate);
				when(tested.entityService.get("1")).thenReturn(new HashMap<String, Object>());
				Map<String, Object> entity = new HashMap<String, Object>();
				entity.put("name", "v1_1");
				entity.put(ContributorService.FIELD_CODE, CODE_1);
				tested.create("1", entity);
				verify(tested.entityService).create("1", entity);
				Map<String, Object> r = indexGetDocument(ContributorService.SEARCH_INDEX_NAME,
						ContributorService.SEARCH_INDEX_TYPE, "2");
				assertNotNull(r);
				assertEquals("v2", r.get("name"));
				r = indexGetDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, "1");
				assertNotNull(r);
				assertEquals("v1_1", r.get("name"));
				verifyZeroInteractions(tested.eventCreate);
				verify(tested.eventUpdate).fire(prepareContributorUpdatedEventMatcher("1", CODE_1));
			}
		} finally {
			indexDelete(ContributorService.SEARCH_INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	@Test(expected = RequiredFieldException.class)
	public void update_codeRequiredValidation() {
		ContributorService tested = getTested(null);

		Map<String, Object> entity = new HashMap<String, Object>();
		entity.put("name", "v1");
		tested.update("1", entity);
	}

	@Test(expected = BadFieldException.class)
	public void update_codeChangeValidation() {
		ContributorService tested = getTested(null);

		Map<String, Object> entity = new HashMap<String, Object>();
		entity.put(ContributorService.FIELD_CODE, "code_1");
		Map<String, Object> oldEntity = new HashMap<>();
		oldEntity.put(ContributorService.FIELD_CODE, "code_2");
		Mockito.when(tested.entityService.get("1")).thenReturn(oldEntity);

		tested.update("1", entity);
	}

	@Test
	public void update() {
		Client client = prepareESClientForUnitTest("ContributorServiceTest_6");
		ContributorService tested = getTested(client);
		try {

			// case - insert noexisting object to noexisting index
			indexDelete(ContributorService.SEARCH_INDEX_NAME);
			{
				Mockito.reset(tested.entityService);
				when(tested.entityService.get("1")).thenReturn(null);
				Map<String, Object> entity = new HashMap<String, Object>();
				entity.put("name", "v1");
				entity.put(ContributorService.FIELD_CODE, CODE_1);

				tested.update("1", entity);
				Mockito.verify(tested.entityService).update("1", entity);
				Map<String, Object> r = indexGetDocument(ContributorService.SEARCH_INDEX_NAME,
						ContributorService.SEARCH_INDEX_TYPE, "1");
				Assert.assertNotNull(r);
				Assert.assertEquals("v1", r.get("name"));

				verify(tested.eventCreate).fire(prepareContributorCreatedEventMatcher("1", CODE_1));
				verifyZeroInteractions(tested.eventUpdate);
			}

			// case - insert noexisting object to existing index
			{
				Mockito.reset(tested.entityService, tested.eventCreate, tested.eventUpdate);
				Map<String, Object> entity = new HashMap<String, Object>();
				entity.put("name", "v2");
				entity.put(ContributorService.FIELD_CODE, CODE_2);
				when(tested.entityService.get("2")).thenReturn(null);
				tested.update("2", entity);
				Mockito.verify(tested.entityService).update("2", entity);
				Map<String, Object> r = indexGetDocument(ContributorService.SEARCH_INDEX_NAME,
						ContributorService.SEARCH_INDEX_TYPE, "2");
				Assert.assertNotNull(r);
				Assert.assertEquals("v2", r.get("name"));
				r = indexGetDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, "1");
				Assert.assertNotNull(r);
				Assert.assertEquals("v1", r.get("name"));
				verify(tested.eventCreate).fire(prepareContributorCreatedEventMatcher("2", CODE_2));
				verifyZeroInteractions(tested.eventUpdate);
			}

			// case - update existing object
			{
				Mockito.reset(tested.entityService, tested.eventCreate, tested.eventUpdate);
				Map<String, Object> entity = new HashMap<String, Object>();
				entity.put("name", "v1_1");
				entity.put(ContributorService.FIELD_CODE, CODE_1);
				when(tested.entityService.get("1")).thenReturn(new HashMap<String, Object>());
				tested.update("1", entity);
				Mockito.verify(tested.entityService).update("1", entity);
				Map<String, Object> r = indexGetDocument(ContributorService.SEARCH_INDEX_NAME,
						ContributorService.SEARCH_INDEX_TYPE, "2");
				Assert.assertNotNull(r);
				Assert.assertEquals("v2", r.get("name"));
				r = indexGetDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, "1");
				Assert.assertNotNull(r);
				Assert.assertEquals("v1_1", r.get("name"));
				verifyZeroInteractions(tested.eventCreate);
				verify(tested.eventUpdate).fire(prepareContributorUpdatedEventMatcher("1", CODE_1));
			}
		} finally {
			indexDelete(ContributorService.SEARCH_INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	@Test
	public void isRolesChanged() {
		ContributorService tested = new ContributorService();
		tested.entityService = Mockito.mock(EntityService.class);

		Map<String, Object> oldEntity = new HashMap<>();
		List<String> oldRoles = new ArrayList<>();
		oldEntity.put(ContributorService.FIELD_ROLES, oldRoles);

		oldRoles.add("admin");
		oldRoles.add("contributor");
		Mockito.when(tested.entityService.get(Mockito.anyString())).thenReturn(oldEntity);


		Map<String, Object> newEntity = new HashMap<>();
		List<String> newRoles = new ArrayList<>();
		newEntity.put(ContributorService.FIELD_ROLES, newRoles);

		newRoles.add("admin");
		newRoles.add("contributor");
		Assert.assertFalse(tested.isRolesChanged("", newEntity));

		newRoles.remove("admin");
		Assert.assertTrue(tested.isRolesChanged("", newEntity));

	}

	@Test
	public void delete() throws InterruptedException {
		Client client = prepareESClientForUnitTest("ContributorServiceTest_7");
		ContributorService tested = getTested(client);
		try {

			indexDelete(ContributorService.SEARCH_INDEX_NAME);
			// case - delete from noexisting index
			{
				reset(tested.entityService);
				tested.delete("1");
				verify(tested.entityService).delete("1");
			}

			indexDelete(ContributorService.SEARCH_INDEX_NAME);
			tested.init();
			Thread.sleep(100);
			// case - index exists but record not in it, no event is fired
			{
				reset(tested.entityService);
				tested.delete("1");
				verify(tested.entityService).delete("1");
				verifyZeroInteractions(tested.eventDelete);
			}

			indexInsertDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, ID_10,
					"{\"name\":\"test1\",\"idx\":\"1\"}");
			indexInsertDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, ID_20,
					"{\"name\":\"test2\",\"idx\":\"2\"}");
			indexInsertDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, ID_30,
					"{\"name\":\"test3\",\"idx\":\"3\"}");
			indexFlushAndRefresh(ContributorService.SEARCH_INDEX_NAME);
			// case - index and record exists, so is record deleted and event fired
			{
				reset(tested.entityService, tested.eventDelete);
				Map<String, Object> entity = new HashMap<>();
				entity.put(ContributorService.FIELD_CODE, CODE_1);
				// mock this to simulate record exists
				Mockito.when(tested.entityService.get(ID_10)).thenReturn(entity);

				tested.delete(ID_10);
				Mockito.verify(tested.entityService).delete(ID_10);
				Mockito.verify(tested.eventDelete).fire(
						Mockito.argThat(new CustomMatcher<ContributorDeletedEvent>(
								"ContributorDeletedEvent [contributorId=10, contributorCode=" + CODE_1 + "]") {

							@Override
							public boolean matches(Object paramObject) {
								ContributorDeletedEvent e = (ContributorDeletedEvent) paramObject;
								return e.getContributorCode().equals(CODE_1) & e.getContributorId().equals(ID_10);
							}

						}));
				Assert.assertNull(indexGetDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE,
						ID_10));
				Assert.assertNotNull(indexGetDocument(ContributorService.SEARCH_INDEX_NAME,
						ContributorService.SEARCH_INDEX_TYPE, ID_20));
				Assert.assertNotNull(indexGetDocument(ContributorService.SEARCH_INDEX_NAME,
						ContributorService.SEARCH_INDEX_TYPE, ID_30));

				Mockito.reset(tested.entityService);
				tested.delete(ID_30);
				Mockito.verify(tested.entityService).delete(ID_30);
				Assert.assertNull(indexGetDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE,
						ID_10));
				Assert.assertNotNull(indexGetDocument(ContributorService.SEARCH_INDEX_NAME,
						ContributorService.SEARCH_INDEX_TYPE, ID_20));
				Assert.assertNull(indexGetDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE,
						ID_30));
			}

		} finally {
			indexDelete(ContributorService.SEARCH_INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	@Test
	public void findByCode_findOneByCode() throws Exception {
		Client client = prepareESClientForUnitTest("ContributorServiceTest_8");
		ContributorService tested = getTested(client);
		try {
			// case - search from noexisting index
			{
				indexDelete(ContributorService.SEARCH_INDEX_NAME);
				Assert.assertNull(tested.findByCode(CODE_1));
				Assert.assertNull(tested.findOneByCode(CODE_1));
			}

			indexDelete(ContributorService.SEARCH_INDEX_NAME);
			tested.init();
			Thread.sleep(100);
			// case - search from empty index
			{
				SearchResponse sr = tested.findByCode(CODE_1);
				Assert.assertEquals(0, sr.getHits().getTotalHits());
				Assert.assertNull(tested.findOneByCode(CODE_1));
			}

			indexInsertDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, ID_10,
					"{\"code\":\"" + CODE_2 + "\",\"email\":\"test2@test.org\"}");
			indexInsertDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, ID_20,
					"{\"code\":\"" + CODE_1 + "\",\"email\":\"test@test.org\"}");
			indexInsertDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, ID_30,
					"{\"code\":\"paul doe <t@te.org>\",\"email\":\"te@te.org\"}");
			indexFlushAndRefresh(ContributorService.SEARCH_INDEX_NAME);
			// case - search existing
			{
				SearchResponse sr = tested.findByCode(CODE_1);
				Assert.assertEquals(1, sr.getHits().getTotalHits());
				Assert.assertEquals(ID_20, sr.getHits().getHits()[0].getId());
				Assert.assertEquals(ID_20, tested.findOneByCode(CODE_1).getId());

				sr = tested.findByCode(CODE_2);
				Assert.assertEquals(1, sr.getHits().getTotalHits());
				Assert.assertEquals(ID_10, sr.getHits().getHits()[0].getId());
				Assert.assertEquals(ID_10, tested.findOneByCode(CODE_2).getId());

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
		Client client = prepareESClientForUnitTest("ContributorServiceTest_9");
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

			indexInsertDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, ID_10,
					"{\"code\":\"test1\",\"email\":\"" + EMAIL_2 + "\"}");
			indexInsertDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, ID_20,
					"{\"code\":\"test2\",\"email\":\"" + EMAIL_1 + "\"}");
			indexInsertDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, ID_30,
					"{\"code\":\"test3\",\"email\":\"he@test.org\"}");
			indexFlushAndRefresh(ContributorService.SEARCH_INDEX_NAME);
			// case - search existing
			{
				SearchResponse sr = tested.findByEmail(EMAIL_1);
				Assert.assertEquals(1, sr.getHits().getTotalHits());
				Assert.assertEquals(ID_20, sr.getHits().getHits()[0].getId());
				List<String> emails = new ArrayList<>();
				emails.add(EMAIL_2);
				Assert.assertEquals(ID_20, tested.findOneByEmail(EMAIL_1, null).getId());

				sr = tested.findByEmail(EMAIL_2);
				Assert.assertEquals(1, sr.getHits().getTotalHits());
				Assert.assertEquals(ID_10, sr.getHits().getHits()[0].getId());
				emails.add(EMAIL_2);
				Assert.assertEquals(ID_10, tested.findOneByEmail(EMAIL_3, emails).getId());

				sr = tested.findByEmail(EMAIL_3);
				Assert.assertEquals(0, sr.getHits().getTotalHits());
			}

		} finally {
			indexDelete(ContributorService.SEARCH_INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	private static final String NAME_FIRST_1 = "John";

	private static final String NAME_1 = NAME_FIRST_1 + " Doe";
	private static final String NAME_2 = NAME_FIRST_1 + " Brock";
	private static final String NAME_3 = "Paul Doe";
	private static final String NAME_4 = "Arthur C. Hoenge";

	@Test
	public void findByName_fulltext() throws Exception {
		Client client = prepareESClientForUnitTest("ContributorServiceTest_10");
		ContributorService tested = getTested(client);
		try {
			// case - search from noexisting index
			{
				indexDelete(ContributorService.SEARCH_INDEX_NAME);
				Assert.assertNull(tested.findByName(NAME_1, false));
			}

			tested.init();
			Thread.sleep(100);
			// case - search from empty index
			{
				SearchResponse sr = tested.findByName(NAME_1, false);
				Assert.assertEquals(0, sr.getHits().getTotalHits());
			}

			indexInsertDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, ID_10,
					"{\"code\":\"test1\",\"name\":\"" + NAME_1 + "\"}");
			indexInsertDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, ID_20,
					"{\"code\":\"test2\",\"name\":\"" + NAME_2 + "\"}");
			indexInsertDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, ID_30,
					"{\"code\":\"test3\",\"name\":\"" + NAME_3 + "\"}");
			indexInsertDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, ID_40,
					"{\"code\":\"test4\",\"name\":\"" + NAME_4 + "\"}");

			indexFlushAndRefresh(ContributorService.SEARCH_INDEX_NAME);
			// case - search existing
			{
				SearchResponse sr = tested.findByName(NAME_1, false);
				Assert.assertEquals(3, sr.getHits().getTotalHits());
				Assert.assertEquals(ID_10, sr.getHits().getHits()[0].getId());
				Assert.assertEquals(ID_20, sr.getHits().getHits()[1].getId());
				Assert.assertEquals(ID_30, sr.getHits().getHits()[2].getId());

				sr = tested.findByName(NAME_FIRST_1, false);
				Assert.assertEquals(2, sr.getHits().getTotalHits());
				Assert.assertEquals(ID_10, sr.getHits().getHits()[0].getId());
				Assert.assertEquals(ID_20, sr.getHits().getHits()[1].getId());

				sr = tested.findByName(NAME_3.toUpperCase(), false);
				Assert.assertEquals(2, sr.getHits().getTotalHits());
				Assert.assertEquals(ID_10, sr.getHits().getHits()[0].getId());
				Assert.assertEquals(ID_30, sr.getHits().getHits()[1].getId());

				sr = tested.findByName("brock", false);
				Assert.assertEquals(1, sr.getHits().getTotalHits());
				Assert.assertEquals(ID_20, sr.getHits().getHits()[0].getId());

				sr = tested.findByName(NAME_4.toLowerCase(), false);
				Assert.assertEquals(1, sr.getHits().getTotalHits());
				Assert.assertEquals(ID_40, sr.getHits().getHits()[0].getId());

				sr = tested.findByName("Cor Hue", false);
				Assert.assertEquals(0, sr.getHits().getTotalHits());
			}

		} finally {
			indexDelete(ContributorService.SEARCH_INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	@Test
	public void findByName_exactMatch() throws Exception {
		Client client = prepareESClientForUnitTest("ContributorServiceTest_11");
		ContributorService tested = getTested(client);
		try {
			// case - search from noexisting index
			{
				indexDelete(ContributorService.SEARCH_INDEX_NAME);
				Assert.assertNull(tested.findByName(NAME_1, true));
			}

			tested.init();
			Thread.sleep(100);
			// case - search from empty index
			{
				SearchResponse sr = tested.findByName(NAME_1, true);
				Assert.assertEquals(0, sr.getHits().getTotalHits());
			}

			indexInsertDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, ID_10,
					"{\"code\":\"test1\",\"name\":\"" + NAME_1 + "\"}");
			indexInsertDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, ID_20,
					"{\"code\":\"test2\",\"name\":\"" + NAME_2 + "\"}");
			indexInsertDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, ID_30,
					"{\"code\":\"test3\",\"name\":\"" + NAME_3 + "\"}");
			indexInsertDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, ID_40,
					"{\"code\":\"test4\",\"name\":\"" + NAME_4 + "\"}");

			indexFlushAndRefresh(ContributorService.SEARCH_INDEX_NAME);
			// case - search existing
			{
				SearchResponse sr = tested.findByName(NAME_1, true);
				Assert.assertEquals(1, sr.getHits().getTotalHits());
				Assert.assertEquals(ID_10, sr.getHits().getHits()[0].getId());

				sr = tested.findByName(NAME_2, true);
				Assert.assertEquals(1, sr.getHits().getTotalHits());
				Assert.assertEquals(ID_20, sr.getHits().getHits()[0].getId());

				sr = tested.findByName(NAME_3, true);
				Assert.assertEquals(1, sr.getHits().getTotalHits());
				Assert.assertEquals(ID_30, sr.getHits().getHits()[0].getId());

				sr = tested.findByName(NAME_4, true);
				Assert.assertEquals(1, sr.getHits().getTotalHits());
				Assert.assertEquals(ID_40, sr.getHits().getHits()[0].getId());

				sr = tested.findByName("Cor Hue", true);
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
		Client client = prepareESClientForUnitTest("ContributorServiceTest_12");
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

			indexInsertDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, ID_10,
					"{\"code\":\"test1\",\"email\":\"me@test.org\""
							+ ", \"type_specific_code\" : {\"code_type1\":\"test\",\"code_type2\":[\"ct2_1_1\",\"ct2_1_2\"]}" + "}");
			indexInsertDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, ID_20,
					"{\"code\":\"test2\",\"email\":\"test@test.org\""
							+ ", \"type_specific_code\" : {\"code_type1\":\"ct1_2\",\"code_type2\":[\"ct2_2_1\",\"test\"]}" + "}");
			indexInsertDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, ID_30,
					"{\"code\":\"test3\",\"email\":\"he@test.org\""
							+ ", \"type_specific_code\" : {\"code_type1\":\"ct1_3\",\"code_type2\":[\"ct2_3_1\",\"test_3_2\"]}" + "}");
			indexInsertDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, ID_40,
					"{\"code\":\"test4\",\"email\":\"he@test.org\"" + ", \"type_specific_code\" : {\"code_type1\":\"ct1_4\"}"
							+ "}");
			indexFlushAndRefresh(ContributorService.SEARCH_INDEX_NAME);
			// case - search existing
			{
				SearchResponse sr = tested.findByTypeSpecificCode(CODE_NAME_1, "test");
				Assert.assertEquals(1, sr.getHits().getTotalHits());
				Assert.assertEquals(ID_10, sr.getHits().getHits()[0].getId());

				sr = tested.findByTypeSpecificCode(CODE_NAME_2, "test");
				Assert.assertEquals(1, sr.getHits().getTotalHits());
				Assert.assertEquals(ID_20, sr.getHits().getHits()[0].getId());

				sr = tested.findByTypeSpecificCode(CODE_NAME_2, "te");
				Assert.assertEquals(0, sr.getHits().getTotalHits());
			}

		} finally {
			indexDelete(ContributorService.SEARCH_INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	@Test
	public void findByTypeSpecificCodeExistence() throws InterruptedException {
		Client client = prepareESClientForUnitTest("ContributorServiceTest_13");
		ContributorService tested = getTested(client);
		try {
			// case - search from noexisting index
			indexDelete(ContributorService.SEARCH_INDEX_NAME);
			{
				Assert.assertNull(tested.findByTypeSpecificCodeExistence(CODE_NAME_1));
			}

			tested.init();
			Thread.sleep(100);
			// case - search from empty index
			{
				SearchResponse sr = tested.findByTypeSpecificCodeExistence(CODE_NAME_2);
				Assert.assertEquals(0, sr.getHits().getTotalHits());
			}

			indexInsertDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, ID_10,
					"{\"code\":\"test1\",\"email\":\"me@test.org\""
							+ ", \"type_specific_code\" : {\"code_type1\":\"test\",\"code_type2\":[\"ct2_1_1\",\"ct2_1_2\"]}" + "}");
			indexInsertDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, ID_20,
					"{\"code\":\"test2\",\"email\":\"test@test.org\"" + ", \"type_specific_code\" : {\"code_type2\":\"ct2_2_1\"}"
							+ "}");
			indexInsertDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, ID_30,
					"{\"code\":\"test3\",\"email\":\"he@test.org\"" + ", \"type_specific_code\" : {\"code_type1\":[\"ct1_3\"]}"
							+ "}");
			indexInsertDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, ID_40,
					"{\"code\":\"test4\",\"email\":\"he@test.org\"" + "}");
			indexFlushAndRefresh(ContributorService.SEARCH_INDEX_NAME);
			// case - search existing
			{

				SearchResponse sr = tested.findByTypeSpecificCodeExistence(CODE_NAME_1);
				Assert.assertEquals(2, sr.getHits().getTotalHits());

				// we use scroll indexer there
				sr = tested.searchClientService.executeESScrollSearchNextRequest(sr);
				Assert.assertEquals(2, sr.getHits().hits().length);
				Assert.assertEquals(ID_10, sr.getHits().getHits()[0].getId());
				Assert.assertEquals(ID_30, sr.getHits().getHits()[1].getId());

				sr = tested.searchClientService.executeESScrollSearchNextRequest(sr);
				Assert.assertEquals(0, sr.getHits().hits().length);

				// second ID
				sr = tested.findByTypeSpecificCodeExistence(CODE_NAME_2);
				Assert.assertEquals(2, sr.getHits().getTotalHits());

				sr = tested.searchClientService.executeESScrollSearchNextRequest(sr);
				Assert.assertEquals(2, sr.getHits().hits().length);
				Assert.assertEquals(ID_10, sr.getHits().getHits()[0].getId());
				Assert.assertEquals(ID_20, sr.getHits().getHits()[1].getId());
				sr = tested.searchClientService.executeESScrollSearchNextRequest(sr);
				Assert.assertEquals(0, sr.getHits().hits().length);

				// unknown ID
				sr = tested.findByTypeSpecificCodeExistence("unknown");
				Assert.assertEquals(0, sr.getHits().getTotalHits());

			}

		} finally {
			indexDelete(ContributorService.SEARCH_INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	@Test
	public void findOneByTypeSpecificCode() throws InterruptedException {
		Client client = prepareESClientForUnitTest("ContributorServiceTest_14");
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

			indexInsertDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, ID_10,
					"{\"code\":\"test1\",\"email\":\"me@test.org\""
							+ ", \"type_specific_code\" : {\"code_type1\":\"test\",\"code_type2\":[\"ct2_1_1\",\"ct2_1_2\"]}" + "}");
			indexInsertDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, ID_20,
					"{\"code\":\"test2\",\"email\":\"test@test.org\""
							+ ", \"type_specific_code\" : {\"code_type1\":\"ct1_2\",\"code_type2\":[\"ct2_2_1\",\"test\"]}" + "}");
			indexInsertDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, ID_30,
					"{\"code\":\"test3\",\"email\":\"he@test.org\""
							+ ", \"type_specific_code\" : {\"code_type1\":\"ct1_3\",\"code_type2\":[\"ct2_3_1\",\"test_3_2\"]}" + "}");
			indexFlushAndRefresh(ContributorService.SEARCH_INDEX_NAME);
			// case - search existing
			{
				Assert.assertEquals(ID_10, tested.findOneByTypeSpecificCode(CODE_NAME_1, "test", null).getId());
				Assert.assertEquals(ID_10, tested.findOneByTypeSpecificCode(CODE_NAME_1, "test", "test1").getId());
				Assert.assertEquals(ID_10, tested.findOneByTypeSpecificCode(CODE_NAME_1, "test", "test2").getId());

				Assert.assertEquals(ID_20, tested.findOneByTypeSpecificCode(CODE_NAME_2, "test", null).getId());

				Assert.assertNull(tested.findOneByTypeSpecificCode(CODE_NAME_2, "te", "test"));
			}

			// case - more contributors for same type specific code code, so we select one by prefered code
			indexInsertDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, ID_40,
					"{\"code\":\"test4\",\"email\":\"me@test.org\""
							+ ", \"type_specific_code\" : {\"code_type1\":\"test\",\"code_type2\":[\"ct2_1_1\",\"ct2_1_2\"]}" + "}");
			indexFlushAndRefresh(ContributorService.SEARCH_INDEX_NAME);
			{
				// first two asserts removed because it is unclear which one will be selected if no prefered code matches
				// Assert.assertEquals("10", tested.findOneByTypeSpecificCode(CODE_NAME_1, "test", null).getId());
				// Assert.assertEquals("10", tested.findOneByTypeSpecificCode(CODE_NAME_1, "test", "unknown").getId());
				Assert.assertEquals(ID_10, tested.findOneByTypeSpecificCode(CODE_NAME_1, "test", "test1").getId());
				Assert.assertEquals(ID_40, tested.findOneByTypeSpecificCode(CODE_NAME_1, "test", "test4").getId());
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
		source.put(ContributorService.FIELD_NAME, "sourcename");
		source.put(ContributorService.FIELD_EMAIL, TestUtils.createListOfStrings(EMAIL_1, EMAIL_2));
		Map<String, List<String>> sourceTSC = new HashMap<>();
		sourceTSC.put(CODE_NAME_1, TestUtils.createListOfStrings("c11_t", "c11"));
		sourceTSC.put(CODE_NAME_2, TestUtils.createListOfStrings("c21"));
		source.put(ContributorService.FIELD_TYPE_SPECIFIC_CODE, sourceTSC);
		source.put(ContributorService.FIELD_ROLES, TestUtils.createListOfStrings("roleA", "roleB"));

		target.put(ContributorService.FIELD_CODE, "targetcode");
		target.put(ContributorService.FIELD_NAME, "targetname");
		target.put(ContributorService.FIELD_EMAIL, TestUtils.createListOfStrings(EMAIL_3, EMAIL_2));
		Map<String, List<String>> targetTSC = new HashMap<>();
		targetTSC.put(CODE_NAME_1, TestUtils.createListOfStrings("c11"));
		target.put(ContributorService.FIELD_TYPE_SPECIFIC_CODE, targetTSC);
		target.put(ContributorService.FIELD_ROLES, TestUtils.createListOfStrings("role1", "role2"));

		tested.mergeContributorData(target, source);

		Assert.assertEquals("sourcecode", ContributorService.getContributorCode(source));
		Assert.assertEquals("targetcode", ContributorService.getContributorCode(target));

		Assert.assertEquals("targetname", target.get(ContributorService.FIELD_NAME));
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
		List roles = (List) target.get(ContributorService.FIELD_ROLES);
		Assert.assertEquals(4, roles.size());
		Assert.assertTrue(roles.contains("roleA"));
		Assert.assertTrue(roles.contains("roleB"));
		Assert.assertTrue(roles.contains("role1"));
		Assert.assertTrue(roles.contains("role2"));
	}

	@Test
	public void mergeContributorData_name_handling() {
		ContributorService tested = getTested(null);

		// case - no problem if no any name defined
		{
			Map<String, Object> source = new HashMap<>();
			Map<String, Object> target = new HashMap<>();
			source.put(ContributorService.FIELD_CODE, "sourcecode");
			target.put(ContributorService.FIELD_CODE, "targetcode");

			tested.mergeContributorData(target, source);

			Assert.assertEquals("sourcecode", ContributorService.getContributorCode(source));
			Assert.assertEquals("targetcode", ContributorService.getContributorCode(target));
			Assert.assertEquals(null, target.get(ContributorService.FIELD_NAME));
		}

		// case - source name is used if target one doesn't exists
		{
			Map<String, Object> source = new HashMap<>();
			Map<String, Object> target = new HashMap<>();
			source.put(ContributorService.FIELD_CODE, "sourcecode");
			source.put(ContributorService.FIELD_NAME, "sourcename");
			target.put(ContributorService.FIELD_CODE, "targetcode");

			tested.mergeContributorData(target, source);

			Assert.assertEquals("sourcecode", ContributorService.getContributorCode(source));
			Assert.assertEquals("targetcode", ContributorService.getContributorCode(target));
			Assert.assertEquals("sourcename", target.get(ContributorService.FIELD_NAME));
		}

		// case - target name is used if exists
		{
			Map<String, Object> source = new HashMap<>();
			Map<String, Object> target = new HashMap<>();
			source.put(ContributorService.FIELD_CODE, "sourcecode");
			source.put(ContributorService.FIELD_NAME, "sourcename");
			target.put(ContributorService.FIELD_CODE, "targetcode");
			target.put(ContributorService.FIELD_NAME, "targetname");

			tested.mergeContributorData(target, source);

			Assert.assertEquals("sourcecode", ContributorService.getContributorCode(source));
			Assert.assertEquals("targetcode", ContributorService.getContributorCode(target));
			Assert.assertEquals("targetname", target.get(ContributorService.FIELD_NAME));
		}
		{
			Map<String, Object> source = new HashMap<>();
			Map<String, Object> target = new HashMap<>();
			source.put(ContributorService.FIELD_CODE, "sourcecode");
			target.put(ContributorService.FIELD_CODE, "targetcode");
			target.put(ContributorService.FIELD_NAME, "targetname");

			tested.mergeContributorData(target, source);

			Assert.assertEquals("sourcecode", ContributorService.getContributorCode(source));
			Assert.assertEquals("targetcode", ContributorService.getContributorCode(target));
			Assert.assertEquals("targetname", target.get(ContributorService.FIELD_NAME));
		}
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
	public void getContributorTypeSpecificCodeFirst() {
		try {
			ContributorService.getContributorTypeSpecificCodeFirst(null, "jbuname");
			Assert.fail("NullPointerException expected");
		} catch (NullPointerException e) {
		}

		Map<String, Object> source = new HashMap<>();
		Assert.assertNull(ContributorService.getContributorTypeSpecificCodeFirst(source, "jbuname"));

		// bad structure
		source.put(ContributorService.FIELD_TYPE_SPECIFIC_CODE, "");
		Assert.assertNull(ContributorService.getContributorTypeSpecificCodeFirst(source, "jbuname"));

		Map<String, Object> tsc = new HashMap<>();
		source.put(ContributorService.FIELD_TYPE_SPECIFIC_CODE, tsc);
		Assert.assertNull(ContributorService.getContributorTypeSpecificCodeFirst(source, "jbuname"));

		tsc.put("jbuname", "fiolunt");
		Assert.assertEquals("fiolunt", ContributorService.getContributorTypeSpecificCodeFirst(source, "jbuname"));

		tsc.put("jbunameint", new Integer(20));
		Assert.assertEquals(ID_20, ContributorService.getContributorTypeSpecificCodeFirst(source, "jbunameint"));

		tsc.put("jbunamemap", new HashMap<>());
		Assert.assertNull(ContributorService.getContributorTypeSpecificCodeFirst(source, "jbunamemap"));

		List<Object> l = new ArrayList<>();
		l.add("guterod");
		l.add("laured");
		tsc.put("secuname", l);
		Assert.assertEquals("guterod", ContributorService.getContributorTypeSpecificCodeFirst(source, "secuname"));

		l.add(0, new Integer(50));
		Assert.assertEquals("50", ContributorService.getContributorTypeSpecificCodeFirst(source, "secuname"));

		Assert.assertEquals("fiolunt", ContributorService.getContributorTypeSpecificCodeFirst(source, "jbuname"));
	}

	@Test
	public void patchEmailUniqueness() throws Exception {
		Client client = prepareESClientForUnitTest("ContributorServiceTest_15");
		ContributorService tested = getTested(client);
		try {
			tested.init();
			Thread.sleep(100);

			indexInsertDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, ID_10,
					"{\"code\":\"test1\",\"email\":[\"me@test.org\",\"you@test.org\"]"
							+ ", \"type_specific_code\" : {\"code_type1\":\"test\",\"code_type2\":[\"ct2_1_1\",\"ct2_1_2\"]}" + "}");
			indexInsertDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, ID_20,
					"{\"code\":\"test2\",\"email\":[\"me@test.org\",\"test@test.org\"]"
							+ ", \"type_specific_code\" : {\"code_type1\":\"ct1_2\",\"code_type2\":[\"ct2_2_1\",\"test\"]}" + "}");
			indexInsertDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, ID_30,
					"{\"code\":\"test3\",\"email\":[\"me@test.org\"]"
							+ ", \"type_specific_code\" : {\"code_type1\":\"ct1_3\",\"code_type2\":[\"ct2_3_1\",\"test_3_2\"]}" + "}");
			indexInsertDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, ID_40,
					"{\"code\":\"test4\",\"email\":[\"no@no.org\",\"you@test.org\"]"
							+ ", \"type_specific_code\" : {\"code_type1\":\"ct1_3\",\"code_type2\":[\"ct2_3_1\",\"test_3_2\"]}" + "}");
			indexInsertDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, "50",
					"{\"code\":\"test5\",\"email\":[\"no@test.org\",\"\"]"
							+ ", \"type_specific_code\" : {\"code_type1\":\"ct1_3\",\"code_type2\":[\"ct2_3_1\",\"test_3_2\"]}" + "}");
			indexFlushAndRefresh(ContributorService.SEARCH_INDEX_NAME);

			Set<String> toRenormalizeContributorIds = new HashSet<>();
			Map<String, Object> contributorEntityContent = new HashMap<>();
			contributorEntityContent.put(ContributorService.FIELD_EMAIL,
					TestUtils.createListOfStrings("me@test.org", "you@test.org", ""));
			tested.patchEmailUniqueness(toRenormalizeContributorIds, ID_10, contributorEntityContent);
			Assert.assertEquals(3, toRenormalizeContributorIds.size());
			Assert.assertTrue(toRenormalizeContributorIds.contains("test2"));
			Assert.assertTrue(toRenormalizeContributorIds.contains("test3"));
			Assert.assertTrue(toRenormalizeContributorIds.contains("test4"));

			// assert content in index
			indexFlushAndRefresh(ContributorService.SEARCH_INDEX_NAME);
			Assert.assertEquals(1, tested.findByEmail("me@test.org").getHits().getTotalHits());
			Assert.assertEquals(1, tested.findByEmail("you@test.org").getHits().getTotalHits());

			TestUtils.assertJsonContent("{\"code\":\"test1\",\"email\":[\"me@test.org\",\"you@test.org\"]"
					+ ", \"type_specific_code\" : {\"code_type1\":\"test\",\"code_type2\":[\"ct2_1_1\",\"ct2_1_2\"]}" + "}",
					indexGetDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, ID_10));
			TestUtils.assertJsonContent("{\"code\":\"test2\",\"email\":[\"test@test.org\"]"
					+ ", \"type_specific_code\" : {\"code_type1\":\"ct1_2\",\"code_type2\":[\"ct2_2_1\",\"test\"]}" + "}",
					indexGetDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, ID_20));
			TestUtils.assertJsonContent("{\"code\":\"test3\",\"email\":[]"
					+ ", \"type_specific_code\" : {\"code_type1\":\"ct1_3\",\"code_type2\":[\"ct2_3_1\",\"test_3_2\"]}" + "}",
					indexGetDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, ID_30));
			TestUtils.assertJsonContent("{\"code\":\"test4\",\"email\":[\"no@no.org\"]"
					+ ", \"type_specific_code\" : {\"code_type1\":\"ct1_3\",\"code_type2\":[\"ct2_3_1\",\"test_3_2\"]}" + "}",
					indexGetDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, ID_40));
			TestUtils.assertJsonContent("{\"code\":\"test5\",\"email\":[\"no@test.org\",\"\"]"
					+ ", \"type_specific_code\" : {\"code_type1\":\"ct1_3\",\"code_type2\":[\"ct2_3_1\",\"test_3_2\"]}" + "}",
					indexGetDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, "50"));

		} finally {
			indexDelete(ContributorService.SEARCH_INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	@Test
	public void patchTypeSpecificCodeUniqueness() throws Exception {
		Client client = prepareESClientForUnitTest("ContributorServiceTest_16");
		ContributorService tested = getTested(client);
		try {
			tested.init();
			Thread.sleep(100);

			indexInsertDocument(
					ContributorService.SEARCH_INDEX_NAME,
					ContributorService.SEARCH_INDEX_TYPE,
					ID_10,
					"{\"code\":\"test1\",\"email\":[\"me@test.org\",\"you@test.org\"]"
							+ ", \"type_specific_code\" : {\"code_type1\":\"test\",\"code_type2\":[\"ct2_1_1\",\"ct2_1_2\",\"test_3_2\"]}"
							+ "}");
			indexInsertDocument(
					ContributorService.SEARCH_INDEX_NAME,
					ContributorService.SEARCH_INDEX_TYPE,
					ID_20,
					"{\"code\":\"test2\",\"email\":[\"me@test.org\",\"test@test.org\"]"
							+ ", \"type_specific_code\" : {\"code_type1\":[\"ct1_2\",\"test\"],\"code_type2\":[\"ct2_2_1\",\"test\"]}"
							+ "}");
			indexInsertDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, ID_30,
					"{\"code\":\"test3\",\"email\":[\"me@test.org\"]"
							+ ", \"type_specific_code\" : {\"code_type1\":\"test\",\"code_type2\":[\"ct2_3_1\",\"test_3_2\"]}" + "}");
			indexInsertDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, ID_40,
					"{\"code\":\"test4\",\"email\":[\"no@no.org\",\"you@test.org\"]"
							+ ", \"type_specific_code\" : {\"code_type1\":\"ct1_3\",\"code_type2\":[\"ct2_3_1\",\"test_3_2\"]}" + "}");
			indexInsertDocument(
					ContributorService.SEARCH_INDEX_NAME,
					ContributorService.SEARCH_INDEX_TYPE,
					"50",
					"{\"code\":\"test5\",\"email\":[\"no@test.org\"]"
							+ ", \"type_specific_code\" : {\"code_type1\":\"ct1_3\",\"code_type2\":[\"ct2_3_1\",\"test_3_2_3\",\"\"]}"
							+ "}");
			indexFlushAndRefresh(ContributorService.SEARCH_INDEX_NAME);

			Set<String> toRenormalizeContributorIds = new HashSet<>();
			Map<String, Object> contributorEntityContent = new HashMap<>();
			Map<String, Object> tsc = new HashMap<>();
			tsc.put(CODE_NAME_1, "test");
			tsc.put(CODE_NAME_2, TestUtils.createListOfStrings("test_3_2", ""));
			contributorEntityContent.put(ContributorService.FIELD_TYPE_SPECIFIC_CODE, tsc);

			tested.patchTypeSpecificCodeUniqueness(toRenormalizeContributorIds, ID_10, contributorEntityContent);
			Assert.assertEquals(3, toRenormalizeContributorIds.size());
			Assert.assertTrue(toRenormalizeContributorIds.contains("test2"));
			Assert.assertTrue(toRenormalizeContributorIds.contains("test3"));
			Assert.assertTrue(toRenormalizeContributorIds.contains("test4"));

			// assert content in index
			indexFlushAndRefresh(ContributorService.SEARCH_INDEX_NAME);
			Assert.assertEquals(1, tested.findByTypeSpecificCode(CODE_NAME_1, "test").getHits().getTotalHits());
			Assert.assertEquals(1, tested.findByTypeSpecificCode(CODE_NAME_2, "test_3_2").getHits().getTotalHits());

			TestUtils
					.assertJsonContent(
							"{\"code\":\"test1\",\"email\":[\"me@test.org\",\"you@test.org\"]"
									+ ", \"type_specific_code\" : {\"code_type1\":\"test\",\"code_type2\":[\"ct2_1_1\",\"ct2_1_2\",\"test_3_2\"]}"
									+ "}",
							indexGetDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, ID_10));

			TestUtils.assertJsonContent("{\"code\":\"test2\",\"email\":[\"me@test.org\",\"test@test.org\"]"
					+ ", \"type_specific_code\" : {\"code_type1\":[\"ct1_2\"],\"code_type2\":[\"ct2_2_1\",\"test\"]}" + "}",
					indexGetDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, ID_20));
			TestUtils.assertJsonContent("{\"code\":\"test3\",\"email\":[\"me@test.org\"]"
					+ ", \"type_specific_code\" : {\"code_type2\":[\"ct2_3_1\"]}" + "}",
					indexGetDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, ID_30));
			TestUtils.assertJsonContent("{\"code\":\"test4\",\"email\":[\"no@no.org\",\"you@test.org\"]"
					+ ", \"type_specific_code\" : {\"code_type1\":\"ct1_3\",\"code_type2\":[\"ct2_3_1\"]}" + "}",
					indexGetDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, ID_40));
			TestUtils.assertJsonContent("{\"code\":\"test5\",\"email\":[\"no@test.org\"]"
					+ ", \"type_specific_code\" : {\"code_type1\":\"ct1_3\",\"code_type2\":[\"ct2_3_1\",\"test_3_2_3\",\"\"]}"
					+ "}", indexGetDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, "50"));

		} finally {
			indexDelete(ContributorService.SEARCH_INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void changeContributorCode() throws Exception {
		Client client = prepareESClientForUnitTest("ContributorServiceTest_17");
		ContributorService tested = getTested(client);
		try {
			tested.init();
			Thread.sleep(100);

			// prepare testing data
			String data1 = "{\"code\":\""
					+ CODE_1
					+ "\",\"email\":[\"me@test.org\",\"john@doe.com\"]"
					+ ", \"type_specific_code\" : {\"code_type1\":\"test\",\"code_type2\":[\"ct2_1_1\",\"ct2_1_2\",\"test_3_2\"]}"
					+ "}";
			String data2 = "{\"code\":\"" + CODE_2 + "\",\"email\":[\"jou@test.org\",\"frank@doe.com\"]"
					+ ", \"type_specific_code\" : {\"code_type1\":\"test1\",\"code_type2\":[\"ct2_1_2\"]}" + "}";

			indexInsertDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, ID_10, data1);
			indexInsertDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, ID_20, data2);
			indexFlushAndRefresh(ContributorService.SEARCH_INDEX_NAME);

			// case - code validation
			{
				resetAllMocks(tested);
				Mockito.when(tested.entityService.get(ID_10)).thenReturn(SearchUtils.convertToJsonMap(data1));
				try {
					tested.changeContributorCode(ID_10, null);
					Assert.fail("RequiredFieldException expected");
				} catch (RequiredFieldException e) {
					verifyNoMoreEventsFired(tested);
				}

				try {
					tested.changeContributorCode(ID_10, " ");
					Assert.fail("RequiredFieldException expected");
				} catch (RequiredFieldException e) {
					verifyNoMoreEventsFired(tested);
				}
			}

			// case - contributor not found
			{
				resetAllMocks(tested);
				Mockito.when(tested.entityService.get(ID_0)).thenReturn(null);
				try {
					tested.changeContributorCode(ID_0, CODE_3);
					Assert.fail("ObjectNotFoundException expected");
				} catch (ObjectNotFoundException e) {
					verifyNoMoreEventsFired(tested);
				}
			}

			// case - code not changed so no any real change performed (no event etc)
			{
				resetAllMocks(tested);
				Mockito.when(tested.entityService.get(ID_10)).thenReturn(SearchUtils.convertToJsonMap(data1));
				Assert.assertNotNull(tested.changeContributorCode(ID_10, CODE_1));

				Mockito.verify(tested.entityService).get(ID_10);
				Mockito.verifyNoMoreInteractions(tested.entityService);
				verifyNoMoreEventsFired(tested);
			}

			// case - new code uniqueness validation
			{
				resetAllMocks(tested);
				Mockito.when(tested.entityService.get(ID_10)).thenReturn(SearchUtils.convertToJsonMap(data1));
				try {
					tested.changeContributorCode(ID_10, CODE_2);
					Assert.fail("BadFieldException expected");
				} catch (BadFieldException e) {
					verifyNoMoreEventsFired(tested);
				}
			}

			// case - update performed and task started
			{
				resetAllMocks(tested);
				Mockito.when(tested.entityService.get(ID_10)).thenReturn(SearchUtils.convertToJsonMap(data1));

				Map<String, Object> ret = tested.changeContributorCode(ID_10, CODE_3);

				Assert.assertEquals(CODE_3, ret.get(ContributorService.FIELD_CODE));

				Mockito.verify(tested.entityService).get(ID_10);
				Mockito.verify(tested.entityService).update(Mockito.eq(ID_10), Mockito.anyMap());
				Mockito.verifyNoMoreInteractions(tested.entityService);

				Mockito.verify(tested.eventContributorCodeChanged).fire(
						prepareContributorCodeChangedEventMatcher(CODE_1, CODE_3));
				verifyNoMoreEventsFired(tested);

				Mockito.verify(tested.taskService.getTaskManager()).createTask(
						Mockito.eq(ReindexingTaskTypes.RENORMALIZE_BY_CONTRIBUTOR_CODE.getTaskType()), Mockito.anyMap());

				indexFlushAndRefresh(ContributorService.SEARCH_INDEX_NAME);
				Assert.assertEquals(0, tested.findByCode(CODE_1).getHits().getTotalHits());
				Assert.assertEquals(1, tested.findByCode(CODE_3).getHits().getTotalHits());
			}

		} finally {
			indexDelete(ContributorService.SEARCH_INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void mergeContributors() throws Exception {
		Client client = prepareESClientForUnitTest("ContributorServiceTest_18");
		ContributorService tested = getTested(client);
		try {
			tested.init();
			Thread.sleep(100);

			// prepare testing data
			String data1 = "{\"code\":\""
					+ CODE_1
					+ "\",\"email\":[\"me@test.org\",\"john@doe.com\"]"
					+ ", \"type_specific_code\" : {\"code_type1\":\"test\",\"code_type2\":[\"ct2_1_1\",\"ct2_1_2\",\"test_3_2\"]}"
					+ "}";
			String data2 = "{\"code\":\"" + CODE_2 + "\",\"email\":[\"jou@test.org\",\"frank@doe.com\"]"
					+ ", \"type_specific_code\" : {\"code_type1\":\"test1\",\"code_type2\":[\"ct2_1_2\"]}" + "}";

			indexInsertDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, ID_10, data1);
			indexInsertDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, ID_20, data2);
			indexFlushAndRefresh(ContributorService.SEARCH_INDEX_NAME);

			// case - contributor From not found
			{
				resetAllMocks(tested);
				Mockito.when(tested.entityService.get(ID_0)).thenReturn(null);
				Mockito.when(tested.entityService.get(ID_10)).thenReturn(SearchUtils.convertToJsonMap(data1));
				try {
					tested.mergeContributors(ID_0, ID_10);
					Assert.fail("ObjectNotFoundException expected");
				} catch (ObjectNotFoundException e) {
					verifyNoMoreEventsFired(tested);
				}
			}

			// case - contributor To not found
			{
				resetAllMocks(tested);
				Mockito.when(tested.entityService.get(ID_0)).thenReturn(null);
				Mockito.when(tested.entityService.get(ID_10)).thenReturn(SearchUtils.convertToJsonMap(data1));
				try {
					tested.mergeContributors(ID_10, ID_0);
					Assert.fail("ObjectNotFoundException expected");
				} catch (ObjectNotFoundException e) {
					verifyNoMoreEventsFired(tested);
				}
			}

			// case - contributor To has no code defined so exception
			{
				resetAllMocks(tested);
				Mockito.when(tested.entityService.get(ID_10)).thenReturn(SearchUtils.convertToJsonMap(data1));
				Map<String, Object> m2 = SearchUtils.convertToJsonMap(data2);
				m2.remove(ContributorService.FIELD_CODE);
				Mockito.when(tested.entityService.get(ID_20)).thenReturn(m2);
				try {
					tested.mergeContributors(ID_10, ID_20);
					Assert.fail("BadFieldException expected");
				} catch (BadFieldException e) {
					verifyNoMoreEventsFired(tested);
				}
			}

			// case - merge performed and task started
			{
				resetAllMocks(tested);
				Mockito.when(tested.entityService.get(ID_10)).thenReturn(SearchUtils.convertToJsonMap(data1));
				Mockito.when(tested.entityService.get(ID_20)).thenReturn(SearchUtils.convertToJsonMap(data2));

				Map<String, Object> ret = tested.mergeContributors(ID_10, ID_20);

				Assert.assertEquals(CODE_2, ret.get(ContributorService.FIELD_CODE));
				TestUtils
						.assertJsonContent(
								"{\"code\":\""
										+ CODE_2
										+ "\","
										+ "\"email\":[\"jou@test.org\",\"frank@doe.com\",\"me@test.org\",\"john@doe.com\"],"
										+ "\"type_specific_code\":{\"code_type1\":[\"test\", \"test1\"],\"code_type2\":[\"ct2_1_2\",\"ct2_1_1\",\"test_3_2\"]}"
										+ "}", ret);

				Mockito.verify(tested.entityService, Mockito.times(2)).get(ID_10);
				Mockito.verify(tested.entityService).get(ID_20);
				Mockito.verify(tested.entityService).update(Mockito.eq(ID_20), Mockito.anyMap());
				Mockito.verify(tested.entityService).delete(ID_10);
				Mockito.verifyNoMoreInteractions(tested.entityService);

				Mockito.verify(tested.eventContributorMerged).fire(prepareContributorMergedEventMatcher(CODE_1, CODE_2));
				verifyNoMoreEventsFired(tested);

				Mockito.verify(tested.taskService.getTaskManager()).createTask(
						Mockito.eq(ReindexingTaskTypes.RENORMALIZE_BY_CONTRIBUTOR_CODE.getTaskType()), Mockito.anyMap());

				indexFlushAndRefresh(ContributorService.SEARCH_INDEX_NAME);
				Assert.assertEquals(0, tested.findByCode(CODE_1).getHits().getTotalHits());
				Assert.assertEquals(1, tested.findByCode(CODE_2).getHits().getTotalHits());
				Assert.assertEquals(0, tested.findByCode(CODE_3).getHits().getTotalHits());
			}

		} finally {
			indexDelete(ContributorService.SEARCH_INDEX_NAME);
			finalizeESClientForUnitTest();
		}

	}

	@SuppressWarnings("unchecked")
	@Test
	public void createOrUpdateFromProfile() throws Exception {
		Client client = prepareESClientForUnitTest("ContributorServiceTest_19");
		ContributorService tested = getTested(client);
		try {
			tested.init();
			Thread.sleep(100);

			// case - create new record
			{
				Mockito.when(tested.entityService.create(Mockito.anyMap())).thenReturn("" + System.currentTimeMillis());

				Map<String, List<String>> typeSpecificCodes = new HashMap<>();
				typeSpecificCodes.put(CODE_NAME_1, TestUtils.createListOfStrings("test"));
				ContributorProfile profile = new ContributorProfile("cpid", "John Doe", "john@doe.com",
						TestUtils.createListOfStrings("john@doe.com", "john@doe.org"), typeSpecificCodes);

				String code = "John Doe <john@doe.com>";
				Assert.assertEquals(code, tested.createOrUpdateFromProfile(profile, null, null));

				indexFlushAndRefresh(ContributorService.SEARCH_INDEX_NAME);
				SearchHit sh = tested.findOneByCode(code);
				Assert.assertNotNull(sh);
				TestUtils
						.assertJsonContent(
								"{\"type_specific_code\":{\"code_type1\":[\"test\"]},\"email\":[\"john@doe.com\",\"john@doe.org\"],\"code\":\"John Doe <john@doe.com>\",\"name\":\"John Doe\"}",
								sh.getSource());
				Mockito.verify(tested.eventCreate, Mockito.times(1)).fire(
						prepareContributorCreatedEventMatcher(sh.getId(), "John Doe <john@doe.com>"));
				verifyNoMoreEventsFired(tested);
			}

			// case - update with only small changes (emails and codes) - check duplicities in other records are patched as
			// necessary
			indexInsertDocument(
					ContributorService.SEARCH_INDEX_NAME,
					ContributorService.SEARCH_INDEX_TYPE,
					ID_10,
					"{\"code\":\"test1\",\"email\":[\"me@test.org\",\"john@doe.com\"]"
							+ ", \"type_specific_code\" : {\"code_type1\":\"test\",\"code_type2\":[\"ct2_1_1\",\"ct2_1_2\",\"test_3_2\"]}"
							+ "}");
			indexFlushAndRefresh(ContributorService.SEARCH_INDEX_NAME);

			{
				resetAllMocks(tested);
				Map<String, List<String>> typeSpecificCodes = new HashMap<>();
				typeSpecificCodes.put(CODE_NAME_1, TestUtils.createListOfStrings("test", "test2"));
				typeSpecificCodes.put(CODE_NAME_2, TestUtils.createListOfStrings("test2"));
				ContributorProfile profile = new ContributorProfile(ID_10, "John Doe", "john@doe.com",
						TestUtils.createListOfStrings("john@doe.com", "john@doere.org"), typeSpecificCodes);

				String code = "John Doe <john@doe.com>";
				Assert.assertEquals(code, tested.createOrUpdateFromProfile(profile, null, null));

				SearchHit sh = tested.findOneByCode(code);
				TestUtils
						.assertJsonContent(
								"{\"type_specific_code\":{\"code_type1\":[\"test\",\"test2\"],\"code_type2\":[\"test2\"]},\"email\":[\"john@doe.com\",\"john@doe.org\",\"john@doere.org\"],\"code\":\"John Doe <john@doe.com>\",\"name\":\"John Doe\"}",
								sh.getSource());
				// assert other record is patched and reindex started
				TestUtils.assertJsonContent("{\"code\":\"test1\",\"email\":[\"me@test.org\"]"
						+ ", \"type_specific_code\" : {\"code_type2\":[\"ct2_1_1\",\"ct2_1_2\",\"test_3_2\"]}" + "}",
						indexGetDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, ID_10));
				Mockito.verify(tested.taskService.getTaskManager()).createTask(
						Mockito.eq(ReindexingTaskTypes.RENORMALIZE_BY_CONTRIBUTOR_CODE.getTaskType()), Mockito.anyMap());

				// update event for updated contributor
				Mockito.verify(tested.eventUpdate).fire(
						prepareContributorUpdatedEventMatcher(sh.getId(), "John Doe <john@doe.com>"));
				// update event is fired for patched Contributor - patched two times (email and code_type1) so two events
				Mockito.verify(tested.eventUpdate, Mockito.times(2))
						.fire(prepareContributorUpdatedEventMatcher(ID_10, "test1"));
				verifyNoMoreEventsFired(tested);
			}

			// case - found by both id and code, updated
			{
				resetAllMocks(tested);
				Map<String, List<String>> typeSpecificCodes = new HashMap<>();
				typeSpecificCodes.put(CODE_NAME_1, TestUtils.createListOfStrings("test", "test2"));
				typeSpecificCodes.put(CODE_NAME_2, TestUtils.createListOfStrings("test2"));
				ContributorProfile profile = new ContributorProfile(ID_10, "John Doe", "john@doe.com",
						TestUtils.createListOfStrings("john@doe.com", "john@doerere.org"), typeSpecificCodes);

				String code = "John Doe <john@doe.com>";
				Assert.assertEquals(code, tested.createOrUpdateFromProfile(profile, CODE_NAME_1, "test"));

				// note that update from profile is additive only, so new email address is added, but old one no more in profile
				// is kept in Contributor record!
				SearchHit sh = tested.findOneByCode(code);
				TestUtils
						.assertJsonContent(
								"{\"type_specific_code\":{\"code_type1\":[\"test\",\"test2\"],\"code_type2\":[\"test2\"]},\"email\":[\"john@doe.com\",\"john@doe.org\",\"john@doere.org\",\"john@doerere.org\"],\"code\":\"John Doe <john@doe.com>\",\"name\":\"John Doe\"}",
								sh.getSource());

				// update event for updated contributor
				Mockito.verify(tested.eventUpdate).fire(
						prepareContributorUpdatedEventMatcher(sh.getId(), "John Doe <john@doe.com>"));
				verifyNoMoreEventsFired(tested);
				Mockito.verifyZeroInteractions(tested.taskService.getTaskManager());
			}

			// case - primary email changed, but we find it over code and update
			{
				resetAllMocks(tested);
				Map<String, List<String>> typeSpecificCodes = new HashMap<>();
				typeSpecificCodes.put(CODE_NAME_1, TestUtils.createListOfStrings("test", "test2"));
				typeSpecificCodes.put(CODE_NAME_2, TestUtils.createListOfStrings("test2", "test3"));
				ContributorProfile profile = new ContributorProfile(ID_10, "John Doe", "jdoe@doe.com",
						TestUtils.createListOfStrings("jdoe@doe.com", "john@doerere.org"), typeSpecificCodes);

				// note that code is not changed in this case
				String code = "John Doe <john@doe.com>";
				Assert.assertEquals(code, tested.createOrUpdateFromProfile(profile, CODE_NAME_1, "test"));

				SearchHit sh = tested.findOneByCode(code);
				TestUtils
						.assertJsonContent(
								"{\"type_specific_code\":{\"code_type1\":[\"test\",\"test2\"],\"code_type2\":[\"test2\",\"test3\"]},\"email\":[\"john@doe.com\",\"john@doe.org\",\"john@doere.org\",\"john@doerere.org\",\"jdoe@doe.com\"],\"code\":\"John Doe <john@doe.com>\",\"name\":\"John Doe\"}",
								sh.getSource());
				// update event for updated contributor
				Mockito.verify(tested.eventUpdate).fire(
						prepareContributorUpdatedEventMatcher(sh.getId(), "John Doe <john@doe.com>"));
				verifyNoMoreEventsFired(tested);
				Mockito.verifyZeroInteractions(tested.taskService.getTaskManager());
			}

			// case - find by email only (name and code changed)
			{
				resetAllMocks(tested);
				Map<String, List<String>> typeSpecificCodes = new HashMap<>();
				ContributorProfile profile = new ContributorProfile(ID_10, "John Doere", "jdoe@doe.com",
						TestUtils.createListOfStrings("jdoe@doe.com", "john@doererere.org"), typeSpecificCodes);

				// note that code is not changed in this case
				String code = "John Doe <john@doe.com>";
				Assert.assertEquals(code, tested.createOrUpdateFromProfile(profile, null, null));

				SearchHit sh = tested.findOneByCode(code);
				TestUtils
						.assertJsonContent(
								"{\"type_specific_code\":{\"code_type1\":[\"test\",\"test2\"],\"code_type2\":[\"test2\",\"test3\"]},\"email\":[\"john@doe.com\",\"john@doe.org\",\"john@doere.org\",\"john@doerere.org\",\"jdoe@doe.com\",\"john@doererere.org\"],\"code\":\"John Doe <john@doe.com>\",\"name\":\"John Doere\"}",
								sh.getSource());
				// update event for updated contributor
				Mockito.verify(tested.eventUpdate).fire(
						prepareContributorUpdatedEventMatcher(sh.getId(), "John Doe <john@doe.com>"));
				verifyNoMoreEventsFired(tested);
				Mockito.verifyZeroInteractions(tested.taskService.getTaskManager());
			}

		} finally {
			indexDelete(ContributorService.SEARCH_INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void createOrUpdateFromProfile_merge() throws Exception {
		Client client = prepareESClientForUnitTest("ContributorServiceTest_20");
		ContributorService tested = getTested(client);
		try {
			tested.init();
			Thread.sleep(100);

			// case - find two distinct records by id and code, so we have to merge them
			indexInsertDocument(
					ContributorService.SEARCH_INDEX_NAME,
					ContributorService.SEARCH_INDEX_TYPE,
					ID_10,
					"{\"type_specific_code\":{\"code_type1\":[\"test_2\"]},\"email\":[\"john@doe.com\",\"john@doe.org\"],\"code\":\"John Doe <john@doe.com>\"}");
			indexInsertDocument(
					ContributorService.SEARCH_INDEX_NAME,
					ContributorService.SEARCH_INDEX_TYPE,
					ID_20,
					"{\"type_specific_code\":{\"code_type1\":[\"test\"],\"code_type3\":[\"test_3\"]},\"email\":[\"john@doe.org\",\"jdoe@doe.org\"],\"code\":\"John Doe <john@doe.org>\"}");

			indexFlushAndRefresh(ContributorService.SEARCH_INDEX_NAME);

			Map<String, List<String>> typeSpecificCodes = new HashMap<>();
			typeSpecificCodes.put(CODE_NAME_1, TestUtils.createListOfStrings("test"));
			typeSpecificCodes.put(CODE_NAME_2, TestUtils.createListOfStrings("test2"));
			ContributorProfile profile = new ContributorProfile(ID_10, "John Doe", "john@doe.com",
					TestUtils.createListOfStrings("john@doe.com"), typeSpecificCodes);

			String code = "John Doe <john@doe.com>";
			Assert.assertEquals(code, tested.createOrUpdateFromProfile(profile, CODE_NAME_1, "test"));

			// assert content is merged into first record
			TestUtils
					.assertJsonContent(
							"{\"type_specific_code\":{\"code_type1\":[\"test_2\",\"test\"],\"code_type2\":[\"test2\"],\"code_type3\":[\"test_3\"]},\"email\":[\"john@doe.com\",\"john@doe.org\",\"jdoe@doe.org\"],\"code\":\"John Doe <john@doe.com>\",\"name\":\"John Doe\"}",
							indexGetDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE, ID_10));
			// assert other record is deleted
			Assert.assertNull(indexGetDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE,
					ID_20));
			Mockito.verify(tested.taskService.getTaskManager()).createTask(
					Mockito.eq(ReindexingTaskTypes.RENORMALIZE_BY_CONTRIBUTOR_CODE.getTaskType()), Mockito.anyMap());
			Mockito.verify(tested.eventContributorMerged).fire(
					prepareContributorMergedEventMatcher("John Doe <john@doe.org>", "John Doe <john@doe.com>"));
		} finally {
			indexDelete(ContributorService.SEARCH_INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	private ContributorCreatedEvent prepareContributorCreatedEventMatcher(final String expectedId,
			final String expectedCode) {
		return Mockito.argThat(new CustomMatcher<ContributorCreatedEvent>("ContributorCreatedEvent [contributorId="
				+ expectedId + " contributorCode=" + expectedCode + "]") {

			@Override
			public boolean matches(Object paramObject) {
				ContributorCreatedEvent e = (ContributorCreatedEvent) paramObject;
				return e.getContributorId().equals(expectedId) && e.getContributorCode().equals(expectedCode)
						&& e.getContributorData() != null;
			}

		});
	}

	private ContributorMergedEvent prepareContributorMergedEventMatcher(final String expectedFrom, final String expectedTo) {
		return Mockito.argThat(new CustomMatcher<ContributorMergedEvent>("ContributorMergedEvent [contributorCodeFrom="
				+ expectedFrom + " contributorCodeTo=" + expectedTo + "]") {

			@Override
			public boolean matches(Object paramObject) {
				ContributorMergedEvent e = (ContributorMergedEvent) paramObject;
				return e.getContributorCodeFrom().equals(expectedFrom) && e.getContributorCodeTo().equals(expectedTo);
			}

		});
	}

	private ContributorCodeChangedEvent prepareContributorCodeChangedEventMatcher(final String expectedFrom,
			final String expectedTo) {
		return Mockito.argThat(new CustomMatcher<ContributorCodeChangedEvent>(
				"ContributorCodeChangedEvent [contributorCodeFrom=" + expectedFrom + " contributorCodeTo=" + expectedTo + "]") {

			@Override
			public boolean matches(Object paramObject) {
				ContributorCodeChangedEvent e = (ContributorCodeChangedEvent) paramObject;
				return e.getContributorCodeFrom().equals(expectedFrom) && e.getContributorCodeTo().equals(expectedTo);
			}

		});
	}

	private ContributorUpdatedEvent prepareContributorUpdatedEventMatcher(final String expectedId,
			final String expectedCode) {
		return Mockito.argThat(new CustomMatcher<ContributorUpdatedEvent>("ContributorUpdatedEvent [contributorId="
				+ expectedId + " contributorCode=" + expectedCode + "]") {

			@Override
			public boolean matches(Object paramObject) {
				ContributorUpdatedEvent e = (ContributorUpdatedEvent) paramObject;
				return e.getContributorId().equals(expectedId) && e.getContributorCode().equals(expectedCode)
						&& e.getContributorData() != null;
			}

		});
	}

	@Test
	public void listRequestInit() {
		ContributorService tested = getTested(null);
		ListRequest expected = Mockito.mock(ListRequest.class);
		Mockito.when(tested.entityService.listRequestInit()).thenReturn(expected);
		Assert.assertEquals(expected, tested.listRequestInit());
		Mockito.verify(tested.entityService).listRequestInit();
		Mockito.verifyNoMoreInteractions(tested.entityService);
	}

	@Test
	public void listRequestNext() {
		ContributorService tested = getTested(null);
		ListRequest expected = Mockito.mock(ListRequest.class);
		ListRequest prev = Mockito.mock(ListRequest.class);
		Mockito.when(tested.entityService.listRequestNext(prev)).thenReturn(expected);
		Assert.assertEquals(expected, tested.listRequestNext(prev));
		Mockito.verify(tested.entityService).listRequestNext(prev);
		Mockito.verifyNoMoreInteractions(tested.entityService);
	}

	@Test
	public void prepareBulkRequest_updateSearchIndexBrb() {
		Client client = prepareESClientForUnitTest("ContributorServiceTest_21");
		ContributorService tested = getTested(client);
		try {

			tested.init();

			BulkRequestBuilder brb = tested.prepareBulkRequest();

			tested.updateSearchIndex(brb, "10", prepareEntity("10"));
			tested.updateSearchIndex(brb, "20", prepareEntity("20"));
			tested.updateSearchIndex(brb, "30", prepareEntity("30"));

			brb.execute().actionGet();

			indexFlushAndRefresh(ContributorService.SEARCH_INDEX_NAME);

			Assert.assertNotNull(indexGetDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE,
					"10"));
			Assert.assertNotNull(indexGetDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE,
					"20"));
			Assert.assertNotNull(indexGetDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE,
					"30"));

		} finally {
			indexDelete(ContributorService.SEARCH_INDEX_NAME);
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
		Client client = prepareESClientForUnitTest("ContributorServiceTest_22");
		ContributorService tested = getTested(client);
		try {

			tested.init();

			tested.create("10", prepareEntity("10"));

			tested.create("20", prepareEntity("20"));
			indexFlushAndRefresh(ContributorService.SEARCH_INDEX_NAME);
			Date timestamp = new Date();
			tested.create("30", prepareEntity("30"));
			tested.create("40", prepareEntity("40"));

			tested.deleteOldFromSearchIndex(timestamp);
			indexFlushAndRefresh(ContributorService.SEARCH_INDEX_NAME);

			Assert.assertNull(indexGetDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE,
					"10"));
			Assert.assertNull(indexGetDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE,
					"20"));

			Assert.assertNotNull(indexGetDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE,
					"30"));
			Assert.assertNotNull(indexGetDocument(ContributorService.SEARCH_INDEX_NAME, ContributorService.SEARCH_INDEX_TYPE,
					"40"));

		} finally {
			indexDelete(ContributorService.SEARCH_INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

}
