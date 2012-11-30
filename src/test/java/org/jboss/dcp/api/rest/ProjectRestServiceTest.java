/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.rest;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import junit.framework.Assert;

import org.jboss.dcp.api.service.EntityService;
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

	protected ProjectRestService getTested() {
		ProjectRestService tested = new ProjectRestService();
		RestEntityServiceBaseTest.mockLogger(tested);
		tested.setEntityService(Mockito.mock(EntityService.class));
		return tested;
	}

}
