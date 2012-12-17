/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.rest;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import junit.framework.Assert;

import org.jboss.dcp.api.service.EntityService;
import org.jboss.dcp.api.service.ProviderService;
import org.jboss.dcp.api.service.SecurityService;
import org.jboss.dcp.api.testtools.TestUtils;
import org.junit.Test;
import org.mockito.Mockito;

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
		EntityService es = Mockito.mock(EntityService.class);
		Mockito.when(tested.providerService.getEntityService()).thenReturn(es);
		Assert.assertNull(tested.entityService);
		tested.init();
		Assert.assertEquals(es, tested.entityService);
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

		// case - error
		Mockito.reset(tested.entityService);
		Mockito.when(tested.entityService.getAll(10, 12, ProviderRestService.FIELDS_TO_REMOVE)).thenThrow(
				new RuntimeException("my exception"));
		TestUtils.assertResponseStatus(tested.getAll(10, 12), Status.INTERNAL_SERVER_ERROR);
		Mockito.verify(tested.entityService).getAll(10, 12, ProviderRestService.FIELDS_TO_REMOVE);
		Mockito.verifyNoMoreInteractions(tested.entityService);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void get() {
		ProviderRestService tested = getTested();

		// input parameter is bad
		{
			TestUtils.assertResponseStatus(tested.get(""), Status.BAD_REQUEST);
		}

		// case entity not found
		{
			Mockito.when(tested.entityService.get("ahoj")).thenReturn(null);
			TestUtils.assertResponseStatus(tested.get("ahoj"), Status.NOT_FOUND);
		}

		// case - entity found but authenticated provider has different name and is not superprovider
		{
			Mockito.reset(tested.entityService, tested.providerService, tested.securityService);
			Map<String, Object> m = new HashMap<String, Object>();
			m.put(ProviderService.NAME, "ahoj");
			Mockito.when(tested.entityService.get("ahoj")).thenReturn(m);
			Mockito.when(tested.providerService.isSuperProvider("aa")).thenReturn(false);
			TestUtils.assertResponseStatus(tested.get("ahoj"), Status.FORBIDDEN);
		}

		// case - entity found, authenticated provider has different name but is superprovider
		{
			Mockito.reset(tested.entityService, tested.providerService, tested.securityService);
			Map<String, Object> m = new HashMap<String, Object>();
			m.put(ProviderService.NAME, "ahoj");
			m.put(ProviderService.PASSWORD_HASH, "sdasdasda");
			Mockito.when(tested.entityService.get("ahoj")).thenReturn(m);
			Mockito.when(tested.providerService.isSuperProvider("aa")).thenReturn(true);
			Map<String, Object> r = (Map<String, Object>) tested.get("ahoj");
			Assert.assertEquals(m, r);
			Assert.assertNull("Password hash must be removed!", r.get(ProviderService.PASSWORD_HASH));
		}

		// case - entity found, authenticated provider has same name and is not superprovider
		{
			Mockito.reset(tested.entityService, tested.providerService, tested.securityService);
			Map<String, Object> m = new HashMap<String, Object>();
			m.put(ProviderService.NAME, "aa");
			m.put(ProviderService.PASSWORD_HASH, "sdasdasda");
			Mockito.when(tested.entityService.get("aa")).thenReturn(m);
			Mockito.when(tested.providerService.isSuperProvider("aa")).thenReturn(false);
			Map<String, Object> r = (Map<String, Object>) tested.get("aa");
			Assert.assertEquals(m, r);
			Assert.assertNull("Password hash must be removed!", r.get(ProviderService.PASSWORD_HASH));
		}
	}

	@Test
	public void changePassword() {
		ProviderRestService tested = getTested();

		// input parameter is bad
		{
			TestUtils.assertResponseStatus(tested.changePassword(null, "pwd"), Status.BAD_REQUEST);
			TestUtils.assertResponseStatus(tested.changePassword("", "pwd"), Status.BAD_REQUEST);
			TestUtils.assertResponseStatus(tested.changePassword("aa", null), Status.BAD_REQUEST);
			TestUtils.assertResponseStatus(tested.changePassword("aa", ""), Status.BAD_REQUEST);
			TestUtils.assertResponseStatus(tested.changePassword("aa", "   "), Status.BAD_REQUEST);
			TestUtils.assertResponseStatus(tested.changePassword("aa", "\n   \n"), Status.BAD_REQUEST);
			TestUtils.assertResponseStatus(tested.changePassword(null, null), Status.BAD_REQUEST);
			TestUtils.assertResponseStatus(tested.changePassword("", ""), Status.BAD_REQUEST);
		}

		// case entity not found
		{
			Mockito.when(tested.entityService.get("ahoj")).thenReturn(null);
			TestUtils.assertResponseStatus(tested.changePassword("ahoj", "pwd"), Status.NOT_FOUND);
		}

		// case - provider entity found and is same as caller, caller is not superprovider
		{
			Mockito.reset(tested.entityService, tested.providerService, tested.securityService);
			Map<String, Object> m = new HashMap<String, Object>();
			m.put(ProviderService.NAME, "aa");
			Mockito.when(tested.entityService.get("aa")).thenReturn(m);
			Mockito.when(tested.providerService.isSuperProvider("aa")).thenReturn(false);
			Mockito.when(tested.securityService.createPwdHash("aa", "pwd")).thenReturn("pwdhash");
			// we also check input password is trimmed!
			TestUtils.assertResponseStatus(tested.changePassword("aa", "\n pwd \n"), Status.OK);
			Mockito.verify(tested.entityService).update("aa", m);
			Assert.assertEquals("pwdhash", m.get(ProviderService.PASSWORD_HASH));
		}

		// case - provider entity found but is different from caller, caller is not superprovider
		{
			Mockito.reset(tested.entityService, tested.providerService, tested.securityService);
			Map<String, Object> m = new HashMap<String, Object>();
			m.put(ProviderService.NAME, "ahoj");
			Mockito.when(tested.entityService.get("ahoj")).thenReturn(m);
			Mockito.when(tested.providerService.isSuperProvider("aa")).thenReturn(false);
			TestUtils.assertResponseStatus(tested.changePassword("ahoj", "pwd"), Status.FORBIDDEN);
			Mockito.verify(tested.entityService).get("ahoj");
			Mockito.verifyNoMoreInteractions(tested.entityService);
		}

		// case - provider entity found but is different from caller, caller is superprovider
		{
			Mockito.reset(tested.entityService, tested.providerService, tested.securityService);
			Map<String, Object> m = new HashMap<String, Object>();
			m.put(ProviderService.NAME, "ahoj");
			Mockito.when(tested.entityService.get("ahoj")).thenReturn(m);
			Mockito.when(tested.providerService.isSuperProvider("aa")).thenReturn(true);
			Mockito.when(tested.securityService.createPwdHash("ahoj", "pwd")).thenReturn("pwdhash");
			TestUtils.assertResponseStatus(tested.changePassword("ahoj", "pwd"), Status.OK);
			Mockito.verify(tested.entityService).update("ahoj", m);
			Assert.assertEquals("pwdhash", m.get(ProviderService.PASSWORD_HASH));
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void create_id() {
		ProviderRestService tested = getTested();

		// case - OK, no previously existing entity so new pwd hash used
		{
			Map<String, Object> m = new HashMap<String, Object>();
			m.put(ProviderService.NAME, "name");
			m.put(ProviderService.PASSWORD_HASH, "pwhs");
			Mockito.when(tested.entityService.get("12")).thenReturn(null);
			Map<String, Object> ret = (Map<String, Object>) tested.create("12", m);
			Assert.assertEquals("12", ret.get("id"));
			Assert.assertEquals("pwhs", m.get(ProviderService.PASSWORD_HASH));
			Assert.assertEquals("name", m.get(ProviderService.NAME));
			Mockito.verify(tested.entityService).create("12", m);
			Mockito.verify(tested.entityService).get("12");
			Mockito.verifyNoMoreInteractions(tested.entityService);
		}

		// case - OK, previously existing entity without pwd hash, so new pwd hash used
		{
			Mockito.reset(tested.entityService);
			Map<String, Object> m = new HashMap<String, Object>();
			m.put(ProviderService.NAME, "name");
			m.put(ProviderService.PASSWORD_HASH, "pwhs");
			Mockito.when(tested.entityService.get("12")).thenReturn(new HashMap<String, Object>());
			Map<String, Object> ret = (Map<String, Object>) tested.create("12", m);
			Assert.assertEquals("12", ret.get("id"));
			Assert.assertEquals("pwhs", m.get(ProviderService.PASSWORD_HASH));
			Assert.assertEquals("name", m.get(ProviderService.NAME));
			Mockito.verify(tested.entityService).create("12", m);
			Mockito.verify(tested.entityService).get("12");
			Mockito.verifyNoMoreInteractions(tested.entityService);
		}

		// case - OK, previously existing entity with pwd hash, so old pwd hash preserved
		{
			Mockito.reset(tested.entityService);
			Map<String, Object> m = new HashMap<String, Object>();
			m.put(ProviderService.NAME, "name");
			m.put(ProviderService.PASSWORD_HASH, "pwhs");
			Map<String, Object> entityOld = new HashMap<String, Object>();
			entityOld.put(ProviderService.PASSWORD_HASH, "pwhsold");
			Mockito.when(tested.entityService.get("12")).thenReturn(entityOld);
			Map<String, Object> ret = (Map<String, Object>) tested.create("12", m);
			Assert.assertEquals("12", ret.get("id"));
			Assert.assertEquals("pwhsold", m.get(ProviderService.PASSWORD_HASH));
			Assert.assertEquals("name", m.get(ProviderService.NAME));
			Mockito.verify(tested.entityService).create("12", m);
			Mockito.verify(tested.entityService).get("12");
			Mockito.verifyNoMoreInteractions(tested.entityService);
		}

		// case - OK, previously existing entity with pwd hash, so old pwd hash preserved. new entity without pwd hash
		{
			Mockito.reset(tested.entityService);
			Map<String, Object> m = new HashMap<String, Object>();
			m.put(ProviderService.NAME, "name");
			Map<String, Object> entityOld = new HashMap<String, Object>();
			entityOld.put(ProviderService.PASSWORD_HASH, "pwhsold");
			Mockito.when(tested.entityService.get("12")).thenReturn(entityOld);
			Map<String, Object> ret = (Map<String, Object>) tested.create("12", m);
			Assert.assertEquals("12", ret.get("id"));
			Assert.assertEquals("pwhsold", m.get(ProviderService.PASSWORD_HASH));
			Assert.assertEquals("name", m.get(ProviderService.NAME));
			Mockito.verify(tested.entityService).create("12", m);
			Mockito.verify(tested.entityService).get("12");
			Mockito.verifyNoMoreInteractions(tested.entityService);
		}

		// case - error
		{
			Mockito.reset(tested.entityService);
			Map<String, Object> m = new HashMap<String, Object>();
			Mockito.doThrow(new RuntimeException("my exception")).when(tested.entityService).create("12", m);
			TestUtils.assertResponseStatus(tested.create("12", m), Status.INTERNAL_SERVER_ERROR);
			Mockito.verify(tested.entityService).get("12");
			Mockito.verify(tested.entityService).create("12", m);
			Mockito.verifyNoMoreInteractions(tested.entityService);
		}
	}

	protected ProviderRestService getTested() {
		ProviderRestService tested = new ProviderRestService();
		RestEntityServiceBaseTest.mockLogger(tested);
		tested.providerService = Mockito.mock(ProviderService.class);
		tested.setEntityService(Mockito.mock(EntityService.class));
		tested.securityService = Mockito.mock(SecurityService.class);
		tested.securityContext = Mockito.mock(SecurityContext.class);
		Mockito.when(tested.securityContext.getUserPrincipal()).thenReturn(new Principal() {

			@Override
			public String getName() {
				return "aa";
			}
		});
		return tested;
	}

}
