/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.searchisko.api.rest.exception.RequiredFieldException;
import org.searchisko.api.service.ProviderService;
import org.searchisko.api.service.SecurityService;
import org.searchisko.api.testtools.TestUtils;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Unit test for {@link ProviderRestService}.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ProviderRestServiceTest {

	@Test
	public void init() {
		ProviderRestService tested = new ProviderRestService();
		tested.log = Logger.getLogger("testlogger");
		Assert.assertNull(tested.entityService);
		tested.providerService = Mockito.mock(ProviderService.class);
		// EntityService es = Mockito.mock(ProviderService.class);
		Assert.assertNull(tested.entityService);
		tested.init();
		// Assert.assertEquals(es, tested.entityService);
	}

	@Test
	public void getAll() {
		ProviderRestService tested = getTested();

		// case - OK
		ESDataOnlyResponse res = new ESDataOnlyResponse(null);
		Mockito.when(tested.entityService.getAll(10, 12, ProviderRestService.FIELDS_TO_REMOVE)).thenReturn(res);
		Assert.assertEquals(res, tested.getAll(10, 12));
		Mockito.verify(tested.entityService).getAll(10, 12, ProviderRestService.FIELDS_TO_REMOVE);
		Mockito.verifyNoMoreInteractions(tested.entityService);

		// case - OK, null returned
		Mockito.reset(tested.entityService);
		Mockito.when(tested.entityService.getAll(10, 12, ProviderRestService.FIELDS_TO_REMOVE)).thenReturn(null);
		Assert.assertEquals(null, tested.getAll(10, 12));
		Mockito.verify(tested.entityService).getAll(10, 12, ProviderRestService.FIELDS_TO_REMOVE);
		Mockito.verifyNoMoreInteractions(tested.entityService);
	}

	@Test(expected = RuntimeException.class)
	public void getAll_exceptionFromService() {
		ProviderRestService tested = getTested();

		// case - error
		Mockito.reset(tested.entityService);
		Mockito.when(tested.entityService.getAll(10, 12, ProviderRestService.FIELDS_TO_REMOVE)).thenThrow(
				new RuntimeException("my exception"));
		TestUtils.assertResponseStatus(tested.getAll(10, 12), Status.INTERNAL_SERVER_ERROR);
		Mockito.verify(tested.entityService).getAll(10, 12, ProviderRestService.FIELDS_TO_REMOVE);
		Mockito.verifyNoMoreInteractions(tested.entityService);
	}

	@Test(expected = RequiredFieldException.class)
	public void get_inputParamValidation() {
		ProviderRestService tested = getTested();

		// input parameter is bad
		{
			TestUtils.assertResponseStatus(tested.get(""), Status.BAD_REQUEST);
		}
	}

	@Test(expected = RequiredFieldException.class)
	public void changePassword_inputPatamValidation_1() {
		ProviderRestService tested = getTested();
		TestUtils.assertResponseStatus(tested.changePassword(null, "pwd"), Status.BAD_REQUEST);
	}

	@Test(expected = RequiredFieldException.class)
	public void changePassword_inputPatamValidation_2() {
		ProviderRestService tested = getTested();
		TestUtils.assertResponseStatus(tested.changePassword("", "pwd"), Status.BAD_REQUEST);
	}

	@Test(expected = RequiredFieldException.class)
	public void changePassword_inputPatamValidation_3() {
		ProviderRestService tested = getTested();
		TestUtils.assertResponseStatus(tested.changePassword("aa", null), Status.BAD_REQUEST);
	}

	@Test(expected = RequiredFieldException.class)
	public void changePassword_inputPatamValidation_4() {
		ProviderRestService tested = getTested();
		TestUtils.assertResponseStatus(tested.changePassword("aa", ""), Status.BAD_REQUEST);
	}

	@Test(expected = RequiredFieldException.class)
	public void changePassword_inputPatamValidation_5() {
		ProviderRestService tested = getTested();
		TestUtils.assertResponseStatus(tested.changePassword("aa", "   "), Status.BAD_REQUEST);
	}

	@Test(expected = RequiredFieldException.class)
	public void changePassword_inputPatamValidation_6() {
		ProviderRestService tested = getTested();
		TestUtils.assertResponseStatus(tested.changePassword("aa", "\n   \n"), Status.BAD_REQUEST);
	}

	@Test(expected = RequiredFieldException.class)
	public void changePassword_inputPatamValidation_7() {
		ProviderRestService tested = getTested();
		TestUtils.assertResponseStatus(tested.changePassword(null, null), Status.BAD_REQUEST);
	}

	@Test(expected = RequiredFieldException.class)
	public void changePassword_inputPatamValidation_8() {
		ProviderRestService tested = getTested();
		TestUtils.assertResponseStatus(tested.changePassword("", ""), Status.BAD_REQUEST);
	}

	@Test(expected = RequiredFieldException.class)
	public void create_id_inputParamValidation() {
		ProviderRestService tested = getTested();
		// case - invalid id parameter
		Map<String, Object> m = new HashMap<String, Object>();
		m.put(ProviderService.NAME, "myname");
		TestUtils.assertResponseStatus(tested.create(null, m), Status.BAD_REQUEST);
		TestUtils.assertResponseStatus(tested.create("", m), Status.BAD_REQUEST);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void create_id() {
		ProviderRestService tested = getTested();

		// case - invalid name field in input data
		{
			Map<String, Object> m = new HashMap<String, Object>();
			TestUtils.assertResponseStatus(tested.create("myname", m), Status.BAD_REQUEST);
			m.put(ProviderService.NAME, "");
			TestUtils.assertResponseStatus(tested.create("myname", m), Status.BAD_REQUEST);
		}

		// case - name field in data is not same as id parameter
		{
			Map<String, Object> m = new HashMap<String, Object>();
			m.put(ProviderService.NAME, "myanothername");
			TestUtils.assertResponseStatus(tested.create("myname", m), Status.BAD_REQUEST);
		}

		// case - OK, no previously existing entity so new pwd hash used
		{
			Map<String, Object> m = new HashMap<String, Object>();
			m.put(ProviderService.NAME, "myname");
			m.put(ProviderService.PASSWORD_HASH, "pwhs");
			Mockito.when(tested.entityService.get("myname")).thenReturn(null);
			Map<String, Object> ret = (Map<String, Object>) tested.create("myname", m);
			Assert.assertEquals("myname", ret.get("id"));
			Assert.assertEquals("pwhs", m.get(ProviderService.PASSWORD_HASH));
			Assert.assertEquals("myname", m.get(ProviderService.NAME));
			Mockito.verify(tested.providerService).create("myname", m);
			Mockito.verify(tested.providerService).get("myname");
			Mockito.verifyNoMoreInteractions(tested.entityService);
		}

		// case - OK, previously existing entity without pwd hash, so new pwd hash used
		{
			Mockito.reset(tested.providerService);
			Map<String, Object> m = new HashMap<String, Object>();
			m.put(ProviderService.NAME, "myname");
			m.put(ProviderService.PASSWORD_HASH, "pwhs");
			Mockito.when(tested.entityService.get("12")).thenReturn(new HashMap<String, Object>());
			Map<String, Object> ret = (Map<String, Object>) tested.create("myname", m);
			Assert.assertEquals("myname", ret.get("id"));
			Assert.assertEquals("pwhs", m.get(ProviderService.PASSWORD_HASH));
			Assert.assertEquals("myname", m.get(ProviderService.NAME));
			Mockito.verify(tested.providerService).create("myname", m);
			Mockito.verify(tested.providerService).get("myname");
			Mockito.verifyNoMoreInteractions(tested.entityService);
		}

		// case - OK, previously existing entity with pwd hash, so old pwd hash preserved
		{
			Mockito.reset(tested.providerService);
			Map<String, Object> m = new HashMap<String, Object>();
			m.put(ProviderService.NAME, "myname");
			m.put(ProviderService.PASSWORD_HASH, "pwhs");
			Map<String, Object> entityOld = new HashMap<String, Object>();
			entityOld.put(ProviderService.PASSWORD_HASH, "pwhsold");
			entityOld.put(ProviderService.NAME, "myname");
			Mockito.when(tested.providerService.get("myname")).thenReturn(entityOld);
			Map<String, Object> ret = (Map<String, Object>) tested.create("myname", m);
			Assert.assertEquals("myname", ret.get("id"));
			Assert.assertEquals("pwhsold", m.get(ProviderService.PASSWORD_HASH));
			Assert.assertEquals("myname", m.get(ProviderService.NAME));
			Mockito.verify(tested.providerService).create("myname", m);
			Mockito.verify(tested.providerService).get("myname");
			Mockito.verifyNoMoreInteractions(tested.entityService);
		}

		// case - OK, previously existing entity with pwd hash, so old pwd hash preserved. new entity without pwd hash
		{
			Mockito.reset(tested.providerService);
			Map<String, Object> m = new HashMap<String, Object>();
			m.put(ProviderService.NAME, "myname");
			Map<String, Object> entityOld = new HashMap<String, Object>();
			entityOld.put(ProviderService.PASSWORD_HASH, "pwhsold");
			entityOld.put(ProviderService.NAME, "myname");
			Mockito.when(tested.providerService.get("myname")).thenReturn(entityOld);
			Map<String, Object> ret = (Map<String, Object>) tested.create("myname", m);
			Assert.assertEquals("myname", ret.get("id"));
			Assert.assertEquals("pwhsold", m.get(ProviderService.PASSWORD_HASH));
			Assert.assertEquals("myname", m.get(ProviderService.NAME));
			Mockito.verify(tested.providerService).create("myname", m);
			Mockito.verify(tested.providerService).get("myname");
			Mockito.verifyNoMoreInteractions(tested.entityService);
		}

	}

	@Test(expected = RuntimeException.class)
	public void create_id_errorFromService() {
		ProviderRestService tested = getTested();
		Mockito.reset(tested.providerService);
		Map<String, Object> m = new HashMap<String, Object>();
		m.put(ProviderService.NAME, "myname");
		Mockito.doThrow(new RuntimeException("my exception")).when(tested.providerService).create("myname", m);
		tested.create("myname", m);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void create_noid() {
		ProviderRestService tested = getTested();

		// case - invalid name field in input data
		{
			Map<String, Object> m = new HashMap<String, Object>();
			TestUtils.assertResponseStatus(tested.create(m), Status.BAD_REQUEST);
			m.put(ProviderService.NAME, "");
			TestUtils.assertResponseStatus(tested.create(m), Status.BAD_REQUEST);
		}

		// case - OK, no previously existing entity so new pwd hash used
		{
			Map<String, Object> m = new HashMap<String, Object>();
			m.put(ProviderService.NAME, "myname");
			m.put(ProviderService.PASSWORD_HASH, "pwhs");
			Mockito.when(tested.entityService.get("myname")).thenReturn(null);
			Map<String, Object> ret = (Map<String, Object>) tested.create(m);
			Assert.assertEquals("myname", ret.get("id"));
			Assert.assertEquals("pwhs", m.get(ProviderService.PASSWORD_HASH));
			Assert.assertEquals("myname", m.get(ProviderService.NAME));
			Mockito.verify(tested.providerService).create("myname", m);
			Mockito.verify(tested.providerService).get("myname");
			Mockito.verifyNoMoreInteractions(tested.entityService);
		}

		// case - OK, previously existing entity without pwd hash, so new pwd hash used
		{
			Mockito.reset(tested.providerService);
			Map<String, Object> m = new HashMap<String, Object>();
			m.put(ProviderService.NAME, "myname");
			m.put(ProviderService.PASSWORD_HASH, "pwhs");
			Mockito.when(tested.entityService.get("12")).thenReturn(new HashMap<String, Object>());
			Map<String, Object> ret = (Map<String, Object>) tested.create(m);
			Assert.assertEquals("myname", ret.get("id"));
			Assert.assertEquals("pwhs", m.get(ProviderService.PASSWORD_HASH));
			Assert.assertEquals("myname", m.get(ProviderService.NAME));
			Mockito.verify(tested.providerService).create("myname", m);
			Mockito.verify(tested.providerService).get("myname");
			Mockito.verifyNoMoreInteractions(tested.entityService);
		}

		// case - OK, previously existing entity with pwd hash, so old pwd hash preserved
		{
			Mockito.reset(tested.providerService);
			Map<String, Object> m = new HashMap<String, Object>();
			m.put(ProviderService.NAME, "myname");
			m.put(ProviderService.PASSWORD_HASH, "pwhs");
			Map<String, Object> entityOld = new HashMap<String, Object>();
			entityOld.put(ProviderService.PASSWORD_HASH, "pwhsold");
			entityOld.put(ProviderService.NAME, "myname");
			Mockito.when(tested.providerService.get("myname")).thenReturn(entityOld);
			Map<String, Object> ret = (Map<String, Object>) tested.create(m);
			Assert.assertEquals("myname", ret.get("id"));
			Assert.assertEquals("pwhsold", m.get(ProviderService.PASSWORD_HASH));
			Assert.assertEquals("myname", m.get(ProviderService.NAME));
			Mockito.verify(tested.providerService).create("myname", m);
			Mockito.verify(tested.providerService).get("myname");
			Mockito.verifyNoMoreInteractions(tested.entityService);
		}

		// case - OK, previously existing entity with pwd hash, so old pwd hash preserved. new entity without pwd hash
		{
			Mockito.reset(tested.providerService);
			Map<String, Object> m = new HashMap<String, Object>();
			m.put(ProviderService.NAME, "myname");
			Map<String, Object> entityOld = new HashMap<String, Object>();
			entityOld.put(ProviderService.PASSWORD_HASH, "pwhsold");
			entityOld.put(ProviderService.NAME, "myname");
			Mockito.when(tested.providerService.get("myname")).thenReturn(entityOld);
			Map<String, Object> ret = (Map<String, Object>) tested.create(m);
			Assert.assertEquals("myname", ret.get("id"));
			Assert.assertEquals("pwhsold", m.get(ProviderService.PASSWORD_HASH));
			Assert.assertEquals("myname", m.get(ProviderService.NAME));
			Mockito.verify(tested.providerService).create("myname", m);
			Mockito.verify(tested.providerService).get("myname");
			Mockito.verifyNoMoreInteractions(tested.providerService);
		}

	}

	@Test(expected = RuntimeException.class)
	public void create_noid_exceptionFromService() {
		ProviderRestService tested = getTested();
		Mockito.reset(tested.providerService);
		Map<String, Object> m = new HashMap<String, Object>();
		m.put(ProviderService.NAME, "myname");
		Mockito.doThrow(new RuntimeException("my exception")).when(tested.providerService).create("myname", m);
		tested.create(m);
	}


	protected ProviderRestService getTested() {
		ProviderRestService tested = new ProviderRestService();
		RestEntityServiceBaseTest.mockLogger(tested);
		tested.providerService = Mockito.mock(ProviderService.class);
		tested.setEntityService(Mockito.mock(ProviderService.class));
		tested.securityService = Mockito.mock(SecurityService.class);
		tested.securityContext = Mockito.mock(SecurityContext.class);
		return tested;
	}

}
