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

		// case - invalid id parameter
		{
			Map<String, Object> m = new HashMap<String, Object>();
			m.put(ProviderService.NAME, "myname");
			TestUtils.assertResponseStatus(tested.create(null, m), Status.BAD_REQUEST);
			TestUtils.assertResponseStatus(tested.create("", m), Status.BAD_REQUEST);
		}

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

		// case - error
		{
			Mockito.reset(tested.providerService);
			Map<String, Object> m = new HashMap<String, Object>();
			m.put(ProviderService.NAME, "myname");
			Mockito.doThrow(new RuntimeException("my exception")).when(tested.providerService).create("myname", m);
			TestUtils.assertResponseStatus(tested.create("myname", m), Status.INTERNAL_SERVER_ERROR);
			Mockito.verify(tested.providerService).get("myname");
			Mockito.verify(tested.providerService).create("myname", m);
			Mockito.verifyNoMoreInteractions(tested.entityService);
		}
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

		// case - error
		{
			Mockito.reset(tested.providerService);
			Map<String, Object> m = new HashMap<String, Object>();
			m.put(ProviderService.NAME, "myname");
			Mockito.doThrow(new RuntimeException("my exception")).when(tested.providerService).create("myname", m);
			TestUtils.assertResponseStatus(tested.create(m), Status.INTERNAL_SERVER_ERROR);
			Mockito.verify(tested.providerService).get("myname");
			Mockito.verify(tested.providerService).create("myname", m);
			Mockito.verifyNoMoreInteractions(tested.providerService);
		}
	}

	@Test
	public void getAll_permissions() {
		TestUtils.assertPermissionSuperProvider(ProviderRestService.class, "getAll", Integer.class, Integer.class);
	}

	@Test
	public void get_permissions() {
		TestUtils.assertPermissionProvider(ProviderRestService.class, "get", String.class);
	}

	@Test
	public void create_permissions() {
		TestUtils.assertPermissionSuperProvider(ProviderRestService.class, "create", String.class, Map.class);
		TestUtils.assertPermissionSuperProvider(ProviderRestService.class, "create", Map.class);
	}

	@Test
	public void delete_permissions() {
		TestUtils.assertPermissionSuperProvider(ProviderRestService.class, "delete", String.class);
	}

	@Test
	public void changePassword_permissions() {
		TestUtils.assertPermissionProvider(ProviderRestService.class, "changePassword", String.class, String.class);
	}

	protected ProviderRestService getTested() {
		ProviderRestService tested = new ProviderRestService();
		RestEntityServiceBaseTest.mockLogger(tested);
		tested.providerService = Mockito.mock(ProviderService.class);
		tested.setEntityService(Mockito.mock(ProviderService.class));
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
