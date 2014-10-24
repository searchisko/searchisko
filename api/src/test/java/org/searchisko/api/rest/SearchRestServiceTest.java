/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.xcontent.ToXContent.Params;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.indices.IndexMissingException;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.searchisko.api.model.QuerySettings;
import org.searchisko.api.rest.exception.BadFieldException;
import org.searchisko.api.rest.exception.RequiredFieldException;
import org.searchisko.api.service.SearchService;
import org.searchisko.api.service.StatsRecordType;
import org.searchisko.api.testtools.TestUtils;
import org.searchisko.api.util.QuerySettingsParser;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * Unit test for {@link SearchRestService}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class SearchRestServiceTest {

	@Test(expected = BadFieldException.class)
	public void search_inputParam_1() throws IOException {
		SearchRestService tested = getTested();
		tested.search(null);
	}

	@Test(expected = BadFieldException.class)
	public void search_invalidParam_2() throws IOException {
		SearchRestService tested = getTested();

		// case - error handling for invalid request value
		Mockito.reset(tested.querySettingsParser, tested.searchService);
		UriInfo uriInfo = Mockito.mock(UriInfo.class);
		MultivaluedMap<String, String> qp = new MultivaluedMapImpl<String, String>();
		Mockito.when(uriInfo.getQueryParameters()).thenReturn(qp);
		Mockito.when(tested.querySettingsParser.parseUriParams(qp)).thenThrow(
				new IllegalArgumentException("test exception"));
		Object response = tested.search(uriInfo);
		TestUtils.assertResponseStatus(response, Status.BAD_REQUEST);
		Mockito.verifyNoMoreInteractions(tested.searchService);
	}

	@Test
	public void search() throws IOException {
		SearchRestService tested = getTested();

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
			Mockito.when(
					tested.searchService.performSearch(Mockito.eq(qs), Mockito.notNull(String.class),
							Mockito.eq(StatsRecordType.SEARCH))).thenReturn(sr);
			Mockito.when(tested.searchService.getIntervalValuesForDateHistogramAggregations(Mockito.eq(qs))).thenReturn(
					new HashMap<String, String>());
			Object response = tested.search(uriInfo);
			Mockito.verify(uriInfo).getQueryParameters();
			Mockito.verify(tested.querySettingsParser).parseUriParams(qp);
			Mockito.verify(tested.searchService).performSearch(Mockito.eq(qs), Mockito.notNull(String.class),
					Mockito.eq(StatsRecordType.SEARCH));
			Mockito.verify(tested.searchService).getIntervalValuesForDateHistogramAggregations(Mockito.eq(qs));
			Mockito.verifyNoMoreInteractions(tested.searchService);
			TestUtils.assetStreamingOutputContentRegexp("\\{\"uuid\":\".+\",\"testfield\":\"testvalue\"\\}", response);
		}

		// case - error handling for index not found exception
		{
			Mockito.reset(tested.querySettingsParser, tested.searchService);
			UriInfo uriInfo = Mockito.mock(UriInfo.class);
			MultivaluedMap<String, String> qp = new MultivaluedMapImpl<String, String>();
			Mockito.when(uriInfo.getQueryParameters()).thenReturn(qp);
			QuerySettings qs = new QuerySettings();
			Mockito.when(tested.querySettingsParser.parseUriParams(qp)).thenReturn(qs);
			Mockito.when(
					tested.searchService.performSearch(Mockito.eq(qs), Mockito.notNull(String.class),
							Mockito.eq(StatsRecordType.SEARCH))).thenThrow(new IndexMissingException(null));
			Object response = tested.search(uriInfo);
			TestUtils.assertResponseStatus(response, Status.NOT_FOUND);
		}
	}

	// case - error handling for other exceptions
	@Test(expected = RuntimeException.class)
	public void search_exceptionFromService() {
		SearchRestService tested = getTested();

		Mockito.reset(tested.querySettingsParser, tested.searchService);
		UriInfo uriInfo = Mockito.mock(UriInfo.class);
		MultivaluedMap<String, String> qp = new MultivaluedMapImpl<String, String>();
		Mockito.when(uriInfo.getQueryParameters()).thenReturn(qp);
		QuerySettings qs = new QuerySettings();
		Mockito.when(tested.querySettingsParser.parseUriParams(qp)).thenReturn(qs);
		Mockito.when(
				tested.searchService.performSearch(Mockito.eq(qs), Mockito.notNull(String.class),
						Mockito.eq(StatsRecordType.SEARCH))).thenThrow(new RuntimeException());
		tested.search(uriInfo);
	}

	@Test(expected = RequiredFieldException.class)
	public void writeSearchHitUsedStatisticsRecord_invalidParam_1() {
		SearchRestService tested = getTested();
		tested.writeSearchHitUsedStatisticsRecord(null, "aa", null);
	}

	public void writeSearchHitUsedStatisticsRecord_invalidParam_2() {
		SearchRestService tested = getTested();
		tested.writeSearchHitUsedStatisticsRecord("", "aa", null);
	}

	public void writeSearchHitUsedStatisticsRecord_invalidParam_3() {
		SearchRestService tested = getTested();
		tested.writeSearchHitUsedStatisticsRecord(" ", "aa", null);
	}

	public void writeSearchHitUsedStatisticsRecord_invalidParam_4() {
		SearchRestService tested = getTested();
		tested.writeSearchHitUsedStatisticsRecord("sss", null, null);
	}

	public void writeSearchHitUsedStatisticsRecord_invalidParam_5() {
		SearchRestService tested = getTested();
		tested.writeSearchHitUsedStatisticsRecord("sss", "", null);
	}

	public void writeSearchHitUsedStatisticsRecord_invalidParam_6() {
		SearchRestService tested = getTested();
		tested.writeSearchHitUsedStatisticsRecord("sss", "  ", null);
	}

	@Test
	public void writeSearchHitUsedStatisticsRecord() {
		SearchRestService tested = getTested();

		// case - input params trimmed
		{
			Mockito.reset(tested.searchService);
			Mockito.when(tested.searchService.writeSearchHitUsedStatisticsRecord("aaa", "bb", null)).thenReturn(true);
			TestUtils.assertResponseStatus(tested.writeSearchHitUsedStatisticsRecord(" aaa ", " bb  ", "  "), Status.OK);
			Mockito.verify(tested.searchService).writeSearchHitUsedStatisticsRecord("aaa", "bb", null);
			Mockito.verifyNoMoreInteractions(tested.searchService);
		}

		// case - record accepted
		{
			Mockito.reset(tested.searchService);
			Mockito.when(tested.searchService.writeSearchHitUsedStatisticsRecord("aaa", "bb", null)).thenReturn(true);
			TestUtils.assertResponseStatus(tested.writeSearchHitUsedStatisticsRecord("aaa", "bb", null), Status.OK,
					"statistics record accepted");
			Mockito.verify(tested.searchService).writeSearchHitUsedStatisticsRecord("aaa", "bb", null);
			Mockito.verifyNoMoreInteractions(tested.searchService);
		}

		// case - record denied
		{
			Mockito.reset(tested.searchService);
			Mockito.when(tested.searchService.writeSearchHitUsedStatisticsRecord("aaa", "bb", "jj")).thenReturn(false);
			TestUtils.assertResponseStatus(tested.writeSearchHitUsedStatisticsRecord("aaa", "bb", "jj"), Status.OK,
					"statistics record ignored");
			Mockito.verify(tested.searchService).writeSearchHitUsedStatisticsRecord("aaa", "bb", "jj");
			Mockito.verifyNoMoreInteractions(tested.searchService);
		}
	}

	@Test(expected = RuntimeException.class)
	public void writeSearchHitUsedStatisticsRecord_exceptionFromService() {
		SearchRestService tested = getTested();
		Mockito.reset(tested.searchService);
		Mockito.when(tested.searchService.writeSearchHitUsedStatisticsRecord("aaa", "bb", null)).thenThrow(
				new RuntimeException("exception"));
		tested.writeSearchHitUsedStatisticsRecord("aaa", "bb", null);
	}

	private SearchRestService getTested() {
		SearchRestService tested = new SearchRestService();
		tested.querySettingsParser = Mockito.mock(QuerySettingsParser.class);
		tested.searchService = Mockito.mock(SearchService.class);
		tested.log = Logger.getLogger("test logger");
		return tested;
	}

}
