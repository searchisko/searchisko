/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.rest;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import junit.framework.Assert;

import org.jboss.dcp.api.service.EntityService;
import org.jboss.dcp.api.service.ProviderService;
import org.jboss.dcp.api.service.SecurityService;
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
		Assert.assertNull(tested.entityService);
		tested.providerService = Mockito.mock(ProviderService.class);
		EntityService es = Mockito.mock(EntityService.class);
		Mockito.when(tested.providerService.getEntityService()).thenReturn(es);
		Assert.assertNull(tested.entityService);
		tested.init();
		Assert.assertEquals(es, tested.entityService);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void get() {
		ProviderRestService tested = getTested();

		// case entity not found
		{
			Mockito.when(tested.entityService.get("ahoj")).thenReturn(null);
			Response r = (Response) tested.get("ahoj");
			Assert.assertEquals(Status.NOT_FOUND.getStatusCode(), r.getStatus());
		}

		// case - entity found but authenticated provider has different name and is not superprovider
		{
			Mockito.reset(tested.entityService, tested.providerService, tested.securityService);
			Map<String, Object> m = new HashMap<String, Object>();
			m.put(ProviderService.NAME, "ahoj");
			Mockito.when(tested.entityService.get("ahoj")).thenReturn(m);
			Mockito.when(tested.providerService.isSuperProvider("aa")).thenReturn(false);
			Response r = (Response) tested.get("ahoj");
			Assert.assertEquals(Status.FORBIDDEN.getStatusCode(), r.getStatus());
		}

		// case - entity found, authenticated provider has different name but is superprovider
		{
			Mockito.reset(tested.entityService, tested.providerService, tested.securityService);
			Map<String, Object> m = new HashMap<String, Object>();
			m.put(ProviderService.NAME, "ahoj");
			Mockito.when(tested.entityService.get("ahoj")).thenReturn(m);
			Mockito.when(tested.providerService.isSuperProvider("aa")).thenReturn(true);
			Map<String, Object> r = (Map<String, Object>) tested.get("ahoj");
			Assert.assertEquals(m, r);
		}

		// case - entity found, authenticated provider has same name and is not superprovider
		{
			Mockito.reset(tested.entityService, tested.providerService, tested.securityService);
			Map<String, Object> m = new HashMap<String, Object>();
			m.put(ProviderService.NAME, "aa");
			Mockito.when(tested.entityService.get("aa")).thenReturn(m);
			Mockito.when(tested.providerService.isSuperProvider("aa")).thenReturn(false);
			Map<String, Object> r = (Map<String, Object>) tested.get("aa");
			Assert.assertEquals(m, r);
		}
	}

	@Test
	public void changePassword() {
		ProviderRestService tested = getTested();

		// case entity not found
		{
			Mockito.when(tested.entityService.get("ahoj")).thenReturn(null);
			Response r = (Response) tested.changePassword("ahoj", "pwd");
			Assert.assertEquals(Status.NOT_FOUND.getStatusCode(), r.getStatus());
		}

		// case - entity found
		{
			Mockito.reset(tested.entityService, tested.providerService, tested.securityService);
			Map<String, Object> m = new HashMap<String, Object>();
			m.put(ProviderService.NAME, "aa");
			Mockito.when(tested.entityService.get("aa")).thenReturn(m);
			Mockito.when(tested.securityService.createPwdHash("aa", "pwd")).thenReturn("pwdhash");
			Response r = (Response) tested.changePassword("aa", "pwd");
			Assert.assertEquals(Status.OK.getStatusCode(), r.getStatus());
			Mockito.verify(tested.entityService).update("aa", m);
			Assert.assertEquals("pwdhash", m.get(ProviderService.PASSWORD_HASH));
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
