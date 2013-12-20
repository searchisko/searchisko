/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import junit.framework.Assert;

import org.elasticsearch.action.search.SearchResponse;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.junit.Test;
import org.mockito.Mockito;
import org.searchisko.api.service.ContributorService;
import org.searchisko.api.testtools.TestUtils;

/**
 * Unit test for {@link ContributorRestService}.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ContributorRestServiceTest {

	@Test
	public void search_inputParamValidation() throws Exception {
		ContributorRestService tested = new ContributorRestService();
		RestEntityServiceBaseTest.mockLogger(tested);

		// only one param with some value is accepted
		TestUtils.assertResponseStatus(tested.search(null), Status.BAD_REQUEST);
		TestUtils.assertResponseStatus(tested.search(prepareUriInfiWithParams()), Status.BAD_REQUEST);
		TestUtils.assertResponseStatus(tested.search(prepareUriInfiWithParams("a", "b", "c", "d")), Status.BAD_REQUEST);
		TestUtils.assertResponseStatus(tested.search(prepareUriInfiWithParams("a", "  ")), Status.BAD_REQUEST);
	}

	@Test
	public void search_byOtherIdentifier() throws Exception {
		ContributorRestService tested = new ContributorRestService();
		tested.contributorService = Mockito.mock(ContributorService.class);
		RestEntityServiceBaseTest.mockLogger(tested);

		// case - return from service OK, one result
		{
			SearchResponse sr = ESDataOnlyResponseTest.mockSearchResponse("ve", "email@em", null, null);
			Mockito.when(tested.contributorService.findByTypeSpecificCode("idType", "idValue")).thenReturn(sr);
			StreamingOutput ret = (StreamingOutput) tested.search(prepareUriInfiWithParams("idType", "idValue"));
			TestUtils.assetStreamingOutputContent(
					"{\"total\":1,\"hits\":[{\"id\":\"ve\",\"data\":{\"sys_name\":\"email@em\",\"sys_id\":\"ve\"}}]}", ret);
		}

		// case - return from service OK, no result
		{
			Mockito.reset(tested.contributorService);
			SearchResponse sr = ESDataOnlyResponseTest.mockSearchResponse(null, null, null, null);
			Mockito.when(tested.contributorService.findByTypeSpecificCode("idType", "idValue")).thenReturn(sr);
			StreamingOutput ret = (StreamingOutput) tested.search(prepareUriInfiWithParams("idType", "idValue"));
			Mockito.verify(tested.contributorService).findByTypeSpecificCode("idType", "idValue");
			TestUtils.assetStreamingOutputContent("{\"total\":0,\"hits\":[]}", ret);
		}

		// case - Exception from service
		try {
			Mockito.reset(tested.contributorService);
			Mockito.when(tested.contributorService.findByTypeSpecificCode("idType", "idValue")).thenThrow(
					new RuntimeException("test exception"));
			TestUtils.assertResponseStatus(tested.search(prepareUriInfiWithParams("idType", "idValue")),
					Status.INTERNAL_SERVER_ERROR);
			Assert.fail("RuntimeException expected");
		} catch (RuntimeException e) {
			// OK
		}
	}

	@Test
	public void search_byEmail() throws Exception {
		ContributorRestService tested = new ContributorRestService();
		tested.contributorService = Mockito.mock(ContributorService.class);
		RestEntityServiceBaseTest.mockLogger(tested);

		// case - return from service OK, one result
		{
			SearchResponse sr = ESDataOnlyResponseTest.mockSearchResponse("ve", "email@em", null, null);
			Mockito.when(tested.contributorService.findByEmail("email@em")).thenReturn(sr);
			StreamingOutput ret = (StreamingOutput) tested.search(prepareUriInfiWithParams(
					ContributorRestService.PARAM_EMAIL, "email@em"));
			TestUtils.assetStreamingOutputContent(
					"{\"total\":1,\"hits\":[{\"id\":\"ve\",\"data\":{\"sys_name\":\"email@em\",\"sys_id\":\"ve\"}}]}", ret);
		}

		// case - return from service OK, no result
		{
			Mockito.reset(tested.contributorService);
			SearchResponse sr = ESDataOnlyResponseTest.mockSearchResponse(null, null, null, null);
			Mockito.when(tested.contributorService.findByEmail("email@em")).thenReturn(sr);
			StreamingOutput ret = (StreamingOutput) tested.search(prepareUriInfiWithParams(
					ContributorRestService.PARAM_EMAIL, "email@em"));
			TestUtils.assetStreamingOutputContent("{\"total\":0,\"hits\":[]}", ret);
		}

		// case - Exception from service
		try {
			Mockito.reset(tested.contributorService);
			Mockito.when(tested.contributorService.findByEmail("email@em")).thenThrow(new RuntimeException("test exception"));
			TestUtils.assertResponseStatus(
					tested.search(prepareUriInfiWithParams(ContributorRestService.PARAM_EMAIL, "email@em")),
					Status.INTERNAL_SERVER_ERROR);
			Assert.fail("RuntimeException expected");
		} catch (RuntimeException e) {
			// OK
		}
	}

	protected UriInfo prepareUriInfiWithParams(String... params) {
		UriInfo uriInfoMock = Mockito.mock(UriInfo.class);
		MultivaluedMap<String, String> qp = new MultivaluedMapImpl<String, String>();
		for (int i = 0; i < params.length; i = i + 2) {
			qp.add(params[i], params[i + 1]);
		}
		Mockito.when(uriInfoMock.getQueryParameters()).thenReturn(qp);
		return uriInfoMock;
	}

	@Test
	public void search_permissions() throws Exception {
		TestUtils.assertPermissionSuperProvider(ContributorRestService.class, "search", UriInfo.class);
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
