/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import java.util.Map;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import junit.framework.Assert;

import org.elasticsearch.action.search.SearchResponse;
import org.searchisko.api.service.ContributorService;
import org.searchisko.api.testtools.TestUtils;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit test for {@link ContributorRestService}.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ContributorRestServiceTest {

	@Test
	public void init() {
		ContributorRestService tested = new ContributorRestService();
		Assert.assertNull(tested.entityService);
		tested.contributorService = Mockito.mock(ContributorService.class);
		Assert.assertNull(tested.entityService);
		tested.init();
		Assert.assertEquals(tested.contributorService, tested.entityService);
	}

	@Test
	public void search_permissions() throws Exception {
		TestUtils.assertPermissionGuest(ContributorRestService.class, "search", String.class);
	}

	@Test
	public void search() throws Exception {
		ContributorRestService tested = new ContributorRestService();
		tested.contributorService = Mockito.mock(ContributorService.class);
		RestEntityServiceBaseTest.mockLogger(tested);

		// case - return from service OK, one result
		{
			SearchResponse sr = ESDataOnlyResponseTest.mockSearchResponse("ve", "email@em", null, null);
			Mockito.when(tested.contributorService.search("email@em")).thenReturn(sr);
			StreamingOutput ret = (StreamingOutput) tested.search("email@em");
			TestUtils.assetStreamingOutputContent(
					"{\"total\":1,\"hits\":[{\"id\":\"ve\",\"data\":{\"sys_name\":\"email@em\",\"sys_id\":\"ve\"}}]}", ret);
		}

		// case - return from service OK, no result
		{
			Mockito.reset(tested.contributorService);
			SearchResponse sr = ESDataOnlyResponseTest.mockSearchResponse(null, null, null, null);
			Mockito.when(tested.contributorService.search("email@em")).thenReturn(sr);
			StreamingOutput ret = (StreamingOutput) tested.search("email@em");
			TestUtils.assetStreamingOutputContent("{\"total\":0,\"hits\":[]}", ret);
		}

		// case - Exception from service
		{
			Mockito.reset(tested.contributorService);
			Mockito.when(tested.contributorService.search("email@em")).thenThrow(new RuntimeException("test exception"));
			TestUtils.assertResponseStatus(tested.search("email@em"), Status.INTERNAL_SERVER_ERROR);
		}
	}

	@Test
	public void getAll_permissions() {
		TestUtils.assertPermissionSuperProvider(ContributorRestService.class, "getAll", Integer.class, Integer.class);
	}

	@Test
	public void get_permissions() {
		TestUtils.assertPermissionSuperProvider(ContributorRestService.class, "get", String.class);
	}

	@Test
	public void create_permissions() {
		TestUtils.assertPermissionSuperProvider(ContributorRestService.class, "create", String.class, Map.class);
		TestUtils.assertPermissionSuperProvider(ContributorRestService.class, "create", Map.class);
	}

	@Test
	public void delete_permissions() {
		TestUtils.assertPermissionSuperProvider(ContributorRestService.class, "delete", String.class);
	}

}
