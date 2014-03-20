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

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.searchisko.api.rest.exception.RequiredFieldException;
import org.searchisko.api.testtools.TestUtils;
import org.searchisko.persistence.service.EntityService;

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

	protected static void mockLogger(RestEntityServiceBase tested) {
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
		try {
			tested.getAll(10, 12);
			Assert.fail("RuntimeException expected");
		} catch (RuntimeException e) {
			Mockito.verify(tested.entityService).getAll(10, 12, null);
			Mockito.verifyNoMoreInteractions(tested.entityService);
		}
	}

	@Test(expected = RequiredFieldException.class)
	public void get_inputParamValidation_1() {
		RestEntityServiceBase tested = getTested();
		tested.get(null);
	}

	@Test(expected = RequiredFieldException.class)
	public void get_inputParamValidation_2() {
		RestEntityServiceBase tested = getTested();
		tested.get("");
	}

	@Test
	public void get() {
		RestEntityServiceBase tested = getTested();

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
		try {
			tested.get("10");
			Assert.fail("RuntimeException expected");
		} catch (RuntimeException e) {
			Mockito.verify(tested.entityService).get("10");
			Mockito.verifyNoMoreInteractions(tested.entityService);
		}
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
		try {
			tested.create(m);
			Assert.fail("RuntimeException expected");
		} catch (RuntimeException e) {
			Mockito.verify(tested.entityService).create(m);
			Mockito.verifyNoMoreInteractions(tested.entityService);
		}
	}

	@Test(expected = RequiredFieldException.class)
	public void create_id_inputParamValidation_1() {
		RestEntityServiceBase tested = getTested();
		Map<String, Object> m = new HashMap<String, Object>();
		TestUtils.assertResponseStatus(tested.create(null, m), Status.BAD_REQUEST);
	}

	@Test(expected = RequiredFieldException.class)
	public void create_id_inputParamValidation_2() {
		RestEntityServiceBase tested = getTested();
		Map<String, Object> m = new HashMap<String, Object>();
		TestUtils.assertResponseStatus(tested.create("", m), Status.BAD_REQUEST);
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
		Mockito.verifyNoMoreInteractions(tested.entityService);

		// case - error
		Mockito.reset(tested.entityService);
		Mockito.doThrow(new RuntimeException("my exception")).when(tested.entityService).create("12", m);
		try {
			tested.create("12", m);
			Assert.fail("RuntimeException expected");
		} catch (RuntimeException e) {
			Mockito.verify(tested.entityService).create("12", m);
			Mockito.verifyNoMoreInteractions(tested.entityService);
		}
	}

	@Test(expected = RequiredFieldException.class)
	public void delete_inputParamValidation_1() {
		RestEntityServiceBase tested = getTested();
		TestUtils.assertResponseStatus(tested.delete(null), Status.BAD_REQUEST);
	}

	@Test(expected = RequiredFieldException.class)
	public void delete_inputParamValidation_2() {
		RestEntityServiceBase tested = getTested();
		TestUtils.assertResponseStatus(tested.delete(""), Status.BAD_REQUEST);
	}

	@Test
	public void delete() {
		RestEntityServiceBase tested = getTested();
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
			try {
				tested.delete("12");
				Assert.fail("RuntimeException expected");
			} catch (RuntimeException e) {
				Mockito.verify(tested.entityService).delete("12");
				Mockito.verifyNoMoreInteractions(tested.entityService);
			}
		}
	}

	// @Test
	// public void createErrorResponse() {
	// RestEntityServiceBase tested = getTested();
	// Response r = TestUtils.assertResponseStatus(tested.createErrorResponse(new Exception("my exception")),
	// Status.INTERNAL_SERVER_ERROR);
	// Assert.assertEquals("Error [java.lang.Exception]: my exception", r.getEntity());
	// }

	@Test
	public void createResponseWithId() {
		RestEntityServiceBase tested = getTested();
		Map<String, Object> m = tested.createResponseWithId("22");
		Assert.assertEquals("22", m.get("id"));
	}

}
