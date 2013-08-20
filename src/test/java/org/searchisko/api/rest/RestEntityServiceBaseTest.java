/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.searchisko.api.testtools.TestUtils;
import org.searchisko.persistence.service.EntityService;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit test for {@link RestEntityServiceBase}.
 *
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class RestEntityServiceBaseTest {

	/**
	 * @return RestEntityServiceBase instance for test with initialized logger
	 */
	protected RestEntityServiceBase getTested() {
		RestEntityServiceBase tested = new RestEntityServiceBase();
		mockLogger(tested);
		tested.setEntityService(Mockito.mock(EntityService.class));
		return tested;
	}

	public static void mockLogger(RestEntityServiceBase tested) {
		tested.log = Logger.getLogger("testlogger");
	}

	@Test
	public void getAll() {
		RestEntityServiceBase tested = getTested();

		// case - OK
		ESDataOnlyResponse res = new ESDataOnlyResponse(null);
		Mockito.when(tested.entityService.getAll(10, 12, null)).thenReturn(res);
		Assert.assertEquals(res, tested.getAll(10, 12));
		Mockito.verify(tested.entityService).getAll(10, 12, null);
		Mockito.verifyNoMoreInteractions(tested.entityService);

		// case - OK, null returned
		Mockito.reset(tested.entityService);
		Mockito.when(tested.entityService.getAll(10, 12, null)).thenReturn(null);
		Assert.assertEquals(null, tested.getAll(10, 12));
		Mockito.verify(tested.entityService).getAll(10, 12, null);
		Mockito.verifyNoMoreInteractions(tested.entityService);

		// case - error
		Mockito.reset(tested.entityService);
		Mockito.when(tested.entityService.getAll(10, 12, null)).thenThrow(new RuntimeException("my exception"));
		TestUtils.assertResponseStatus(tested.getAll(10, 12), Status.INTERNAL_SERVER_ERROR);
		Mockito.verify(tested.entityService).getAll(10, 12, null);
		Mockito.verifyNoMoreInteractions(tested.entityService);
	}

	@Test
	public void get() {
		RestEntityServiceBase tested = getTested();

		// input parameter is bad
		{
			TestUtils.assertResponseStatus(tested.get(null), Status.BAD_REQUEST);
			TestUtils.assertResponseStatus(tested.get(""), Status.BAD_REQUEST);
		}

		// case - OK, object returned
		Map<String, Object> m = new HashMap<String, Object>();
		Mockito.when(tested.entityService.get("10")).thenReturn(m);
		Assert.assertEquals(m, tested.get("10"));
		Mockito.verify(tested.entityService).get("10");
		Mockito.verifyNoMoreInteractions(tested.entityService);

		// case - OK, object not found
		Mockito.reset(tested.entityService);
		Mockito.when(tested.entityService.get("10")).thenReturn(null);
		Assert.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), ((Response) tested.get("10")).getStatus());
		Mockito.verify(tested.entityService).get("10");
		Mockito.verifyNoMoreInteractions(tested.entityService);

		// case - exception from service
		Mockito.reset(tested.entityService);
		Mockito.when(tested.entityService.get("10")).thenThrow(new RuntimeException("my exception"));
		TestUtils.assertResponseStatus(tested.get("10"), Status.INTERNAL_SERVER_ERROR);
		Mockito.verify(tested.entityService).get("10");
		Mockito.verifyNoMoreInteractions(tested.entityService);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void create_noid() {
		RestEntityServiceBase tested = getTested();

		// case - OK
		Map<String, Object> m = new HashMap<String, Object>();
		Mockito.when(tested.entityService.create(m)).thenReturn("12");
		Map<String, Object> ret = (Map<String, Object>) tested.create(m);
		Assert.assertEquals("12", ret.get("id"));
		Mockito.verify(tested.entityService).create(m);
		Mockito.verifyNoMoreInteractions(tested.entityService);

		// case - error
		Mockito.reset(tested.entityService);
		Mockito.when(tested.entityService.create(m)).thenThrow(new RuntimeException("my exception"));
		TestUtils.assertResponseStatus(tested.create(m), Status.INTERNAL_SERVER_ERROR);
		Mockito.verify(tested.entityService).create(m);
		Mockito.verifyNoMoreInteractions(tested.entityService);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void create_id() {
		RestEntityServiceBase tested = getTested();

		// input parameter is bad
		{
			Map<String, Object> m = new HashMap<String, Object>();
			TestUtils.assertResponseStatus(tested.create(null, m), Status.BAD_REQUEST);
			TestUtils.assertResponseStatus(tested.create("", m), Status.BAD_REQUEST);
		}

		// case - OK
		Map<String, Object> m = new HashMap<String, Object>();
		Map<String, Object> ret = (Map<String, Object>) tested.create("12", m);
		Assert.assertEquals("12", ret.get("id"));
		Mockito.verify(tested.entityService).create("12", m);
		Mockito.verifyNoMoreInteractions(tested.entityService);

		// case - error
		Mockito.reset(tested.entityService);
		Mockito.doThrow(new RuntimeException("my exception")).when(tested.entityService).create("12", m);
		TestUtils.assertResponseStatus(tested.create("12", m), Status.INTERNAL_SERVER_ERROR);
		Mockito.verify(tested.entityService).create("12", m);
		Mockito.verifyNoMoreInteractions(tested.entityService);
	}

	@Test
	public void delete() {
		RestEntityServiceBase tested = getTested();
		// input parameter is bad
		{
			TestUtils.assertResponseStatus(tested.delete(null), Status.BAD_REQUEST);
			TestUtils.assertResponseStatus(tested.delete(""), Status.BAD_REQUEST);
		}
		// case - OK
		{
			Response r = (Response) tested.delete("12");
			Assert.assertEquals(Status.OK.getStatusCode(), r.getStatus());
			Mockito.verify(tested.entityService).delete("12");
			Mockito.verifyNoMoreInteractions(tested.entityService);
		}

		// case - error
		{
			Mockito.reset(tested.entityService);
			Mockito.doThrow(new RuntimeException("my exception")).when(tested.entityService).delete("12");
			TestUtils.assertResponseStatus(tested.delete("12"), Status.INTERNAL_SERVER_ERROR);
			Mockito.verify(tested.entityService).delete("12");
			Mockito.verifyNoMoreInteractions(tested.entityService);
		}
	}

//	@Test
//	public void createErrorResponse() {
//		RestEntityServiceBase tested = getTested();
//		Response r = TestUtils.assertResponseStatus(tested.createErrorResponse(new Exception("my exception")),
//				Status.INTERNAL_SERVER_ERROR);
//		Assert.assertEquals("Error [java.lang.Exception]: my exception", r.getEntity());
//	}

	@Test
	public void createResponseWithId() {
		RestEntityServiceBase tested = getTested();
		Map<String, Object> m = tested.createResponseWithId("22");
		Assert.assertEquals("22", m.get("id"));
	}

}
