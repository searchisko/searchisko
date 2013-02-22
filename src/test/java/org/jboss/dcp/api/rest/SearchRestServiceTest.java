/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.rest;

import java.io.IOException;
import java.util.logging.Logger;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.xcontent.ToXContent.Params;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.indices.IndexMissingException;
import org.jboss.dcp.api.model.QuerySettings;
import org.jboss.dcp.api.service.SearchService;
import org.jboss.dcp.api.testtools.TestUtils;
import org.jboss.dcp.api.util.QuerySettingsParser;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Unit test for {@link SearchRestService}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class SearchRestServiceTest {

	@Test
	public void search_permissions() {
		TestUtils.assertPermissionGuest(SearchRestService.class, "search", UriInfo.class);
	}

	@Test
	public void search() throws IOException {
		SearchRestService tested = new SearchRestService();
		tested.querySettingsParser = Mockito.mock(QuerySettingsParser.class);
		tested.searchService = Mockito.mock(SearchService.class);
		tested.log = Logger.getLogger("test logger");

		// case - incorrect input
		{
			Object response = tested.search(null);
			TestUtils.assertResponseStatus(response, Status.INTERNAL_SERVER_ERROR);
		}

		// case - correct processing sequence
		{
			UriInfo uriInfo = Mockito.mock(UriInfo.class);
			MultivaluedMap<String, String> qp = new MultivaluedMapImpl<String, String>();
			Mockito.when(uriInfo.getQueryParameters()).thenReturn(qp);
			QuerySettings qs = new QuerySettings();
			Mockito.when(tested.querySettingsParser.parseUriParams(qp)).thenReturn(qs);
			SearchResponse sr = Mockito.mock(SearchResponse.class);
			Mockito.doAnswer(new Answer<Object>() {

				@Override
				public Object answer(InvocationOnMock invocation) throws Throwable {
					XContentBuilder b = (XContentBuilder) invocation.getArguments()[0];
					b.field("testfield", "testvalue");
					return null;
				}
			}).when(sr).toXContent(Mockito.any(XContentBuilder.class), Mockito.any(Params.class));
			Mockito.when(tested.searchService.performSearch(Mockito.eq(qs), Mockito.notNull(String.class))).thenReturn(sr);
			Object response = tested.search(uriInfo);
			Mockito.verify(uriInfo).getQueryParameters();
			Mockito.verify(tested.querySettingsParser).parseUriParams(qp);
			Mockito.verify(tested.searchService).performSearch(Mockito.eq(qs), Mockito.notNull(String.class));
			TestUtils.assetStreamingOutputContentRegexp("\\{\"uuid\":\".+\",\"testfield\":\"testvalue\"\\}", response);
		}

		// case - error handling for invalid request value
		{
			Mockito.reset(tested.querySettingsParser, tested.searchService);
			UriInfo uriInfo = Mockito.mock(UriInfo.class);
			MultivaluedMap<String, String> qp = new MultivaluedMapImpl<String, String>();
			Mockito.when(uriInfo.getQueryParameters()).thenReturn(qp);
			Mockito.when(tested.querySettingsParser.parseUriParams(qp)).thenThrow(
					new IllegalArgumentException("test exception"));
			Object response = tested.search(uriInfo);
			TestUtils.assertResponseStatus(response, Status.BAD_REQUEST);
		}

		// case - error handling for index not found exception
		{
			Mockito.reset(tested.querySettingsParser, tested.searchService);
			UriInfo uriInfo = Mockito.mock(UriInfo.class);
			MultivaluedMap<String, String> qp = new MultivaluedMapImpl<String, String>();
			Mockito.when(uriInfo.getQueryParameters()).thenReturn(qp);
			QuerySettings qs = new QuerySettings();
			Mockito.when(tested.querySettingsParser.parseUriParams(qp)).thenReturn(qs);
			Mockito.when(tested.searchService.performSearch(Mockito.eq(qs), Mockito.notNull(String.class))).thenThrow(
					new IndexMissingException(null));
			Object response = tested.search(uriInfo);
			TestUtils.assertResponseStatus(response, Status.NOT_FOUND);
		}

		// case - error handling for other exceptions
		{
			Mockito.reset(tested.querySettingsParser, tested.searchService);
			UriInfo uriInfo = Mockito.mock(UriInfo.class);
			MultivaluedMap<String, String> qp = new MultivaluedMapImpl<String, String>();
			Mockito.when(uriInfo.getQueryParameters()).thenReturn(qp);
			QuerySettings qs = new QuerySettings();
			Mockito.when(tested.querySettingsParser.parseUriParams(qp)).thenReturn(qs);
			Mockito.when(tested.searchService.performSearch(Mockito.eq(qs), Mockito.notNull(String.class))).thenThrow(
					new RuntimeException());
			Object response = tested.search(uriInfo);
			TestUtils.assertResponseStatus(response, Status.INTERNAL_SERVER_ERROR);
		}
	}
}
