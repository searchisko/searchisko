/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.rest;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.dcp.api.service.EntityService;
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
		Mockito.when(tested.entityService.getAll(10, 12)).thenReturn("OK");
		Assert.assertEquals("OK", tested.getAll(10, 12));

		// case - error
		Mockito.reset(tested.entityService);
		Mockito.when(tested.entityService.getAll(10, 12)).thenThrow(new RuntimeException("my exception"));
		Response r = (Response) tested.getAll(10, 12);
		Assert.assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), r.getStatus());
	}

	@Test
	public void get() {
		RestEntityServiceBase tested = getTested();

		// case - OK
		Map<String, Object> m = new HashMap<String, Object>();
		Mockito.when(tested.entityService.get("10")).thenReturn(m);
		Assert.assertEquals(m, tested.get("10"));

		// case - error
		Mockito.reset(tested.entityService);
		Mockito.when(tested.entityService.get("10")).thenThrow(new RuntimeException("my exception"));
		Response r = (Response) tested.get("10");
		Assert.assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), r.getStatus());
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

		// case - error
		Mockito.reset(tested.entityService);
		Mockito.when(tested.entityService.create(m)).thenThrow(new RuntimeException("my exception"));
		Response r = (Response) tested.create(m);
		Assert.assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), r.getStatus());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void create_id() {
		RestEntityServiceBase tested = getTested();

		// case - OK
		Map<String, Object> m = new HashMap<String, Object>();
		Map<String, Object> ret = (Map<String, Object>) tested.create("12", m);
		Assert.assertEquals("12", ret.get("id"));
		Mockito.verify(tested.entityService).create("12", m);

		// case - error
		Mockito.reset(tested.entityService);
		Mockito.doThrow(new RuntimeException("my exception")).when(tested.entityService).create("12", m);
		Response r = (Response) tested.create("12", m);
		Assert.assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), r.getStatus());
	}

	@Test
	public void delete() {
		RestEntityServiceBase tested = getTested();

		// case - OK
		{
			Response r = (Response) tested.delete("12");
			Assert.assertEquals(Status.OK.getStatusCode(), r.getStatus());
			Mockito.verify(tested.entityService).delete("12");
		}

		// case - error
		{
			Mockito.reset(tested.entityService);
			Mockito.doThrow(new RuntimeException("my exception")).when(tested.entityService).delete("12");
			Response r = (Response) tested.delete("12");
			Assert.assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), r.getStatus());
		}
	}

	@Test
	public void createErrorResponse() {
		RestEntityServiceBase tested = getTested();
		Response r = tested.createErrorResponse(new Exception("my exception"));
		Assert.assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), r.getStatus());
		Assert.assertEquals("Error [java.lang.Exception]: my exception", r.getEntity());
	}

	@Test
	public void createResponseWithId() {
		RestEntityServiceBase tested = getTested();
		Map<String, Object> m = tested.createResponseWithId("22");
		Assert.assertEquals("22", m.get("id"));
	}

}
