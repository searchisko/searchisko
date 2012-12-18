/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.rest;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import junit.framework.Assert;

import org.jboss.dcp.api.service.EntityService;
import org.jboss.dcp.api.testtools.TestUtils;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit test for {@link ProjectRestService}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ProjectRestServiceTest {

	@Test
	public void init() {
		ProjectRestService tested = new ProjectRestService();
		Assert.assertNull(tested.entityService);
		tested.projectService = Mockito.mock(EntityService.class);
		Assert.assertNull(tested.entityService);
		tested.init();
		Assert.assertEquals(tested.projectService, tested.entityService);
	}

	@Test
	public void getAll() {
		ProjectRestService tested = getTested();

		// case - OK
		ESDataOnlyResponse res = new ESDataOnlyResponse(null);
		Mockito.when(tested.entityService.getAll(10, 12, tested.fieldsToRemove)).thenReturn(res);
		Assert.assertEquals(res, tested.getAll(10, 12));

		// case - error
		Mockito.reset(tested.entityService);
		Mockito.when(tested.entityService.getAll(10, 12, tested.fieldsToRemove)).thenThrow(
				new RuntimeException("my exception"));
		TestUtils.assertResponseStatus(tested.getAll(10, 12), Status.INTERNAL_SERVER_ERROR);
	}

	@Test
	public void get() {
		ProjectRestService tested = getTested();

		// case - OK
		Map<String, Object> m = new HashMap<String, Object>();
		Mockito.when(tested.entityService.get("10")).thenReturn(m);
		Assert.assertEquals(m, tested.get("10"));

		// case - error
		Mockito.reset(tested.entityService);
		Mockito.when(tested.entityService.get("10")).thenThrow(new RuntimeException("my exception"));
		TestUtils.assertResponseStatus(tested.get("10"), Status.INTERNAL_SERVER_ERROR);
	}

	@Test
	public void getAll_permissions() {
		TestUtils.assertPermissionGuest(ProjectRestService.class, "getAll", Integer.class, Integer.class);
	}

	@Test
	public void get_permissions() {
		TestUtils.assertPermissionGuest(ProjectRestService.class, "get", String.class);
	}

	@Test
	public void create_permissions() {
		TestUtils.assertPermissionSuperProvider(ProjectRestService.class, "create", String.class, Map.class);
		TestUtils.assertPermissionSuperProvider(ProjectRestService.class, "create", Map.class);
	}

	@Test
	public void delete_permissions() {
		TestUtils.assertPermissionSuperProvider(ProjectRestService.class, "delete", String.class);
	}

	protected ProjectRestService getTested() {
		ProjectRestService tested = new ProjectRestService();
		RestEntityServiceBaseTest.mockLogger(tested);
		tested.setEntityService(Mockito.mock(EntityService.class));
		tested.securityContext = Mockito.mock(SecurityContext.class);
		return tested;
	}

}
