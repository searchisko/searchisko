/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.rest;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.logging.Logger;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import junit.framework.Assert;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.SearchHits;
import org.jboss.dcp.api.model.FacetValue;
import org.jboss.dcp.api.model.PastIntervalValue;
import org.jboss.dcp.api.model.QuerySettings;
import org.jboss.dcp.api.model.QuerySettings.Filters;
import org.jboss.dcp.api.model.SortByValue;
import org.jboss.dcp.api.service.SearchService;
import org.jboss.dcp.api.service.StatsRecordType;
import org.jboss.dcp.api.testtools.TestUtils;
import org.jboss.dcp.api.util.QuerySettingsParser;
import org.jboss.resteasy.plugins.providers.atom.Feed;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit test for {@link FeedRestService}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class FeedRestServiceTest {

	@Test
	public void feed_permissions() {
		TestUtils.assertPermissionGuest(FeedRestService.class, "feed", UriInfo.class);
	}

	@Test
	public void patchQuerySettings() {
		FeedRestService tested = getTested();

		try {
			tested.patchQuerySettings(null);
			Assert.fail("NullPointerException expected");
		} catch (NullPointerException e) {
			// OK
		}

		// case - set some values on empty QuerySettings
		{
			QuerySettings qs = new QuerySettings();
			tested.patchQuerySettings(qs);

			// assert patched fields
			Assert.assertEquals(false, qs.isQueryHighlight());
			Assert.assertEquals(new Integer(0), qs.getFilters().getFrom());
			Assert.assertEquals(new Integer(20), qs.getFilters().getSize());
			Assert.assertEquals(SortByValue.NEW, qs.getSortBy());
			Assert.assertEquals(9, qs.getFields().size());
			Assert.assertNull(qs.getFilters().getActivityDateFrom());
			Assert.assertNull(qs.getFilters().getActivityDateTo());
			Assert.assertNull(qs.getFilters().getActivityDateInterval());

			// assert preserved fields
			Assert.assertEquals(null, qs.getFacets());
			Assert.assertNull(qs.getFilters().getContentType());
			Assert.assertNull(qs.getFilters().getDcpContentProvider());
			Assert.assertNull(qs.getFilters().getContributors());
			Assert.assertNull(qs.getFilters().getDcpTypes());
			Assert.assertNull(qs.getFilters().getProjects());
			Assert.assertNull(qs.getFilters().getTags());
		}

		// case - set and reset some values on nonempty QuerySettings
		{
			QuerySettings qs = new QuerySettings();
			qs.addFacet(FacetValue.ACTIVITY_DATES_HISTOGRAM);
			qs.addFacet(FacetValue.PER_PROJECT_COUNTS);
			qs.addField("field1");
			qs.addField("field2");
			qs.setQueryHighlight(true);
			qs.setQuery("Querry");
			// unsupported sort by must be changed
			qs.setSortBy(SortByValue.OLD);
			Filters f = qs.getFiltersInit();
			f.setFrom(50);
			f.setSize(150);
			f.setActivityDateFrom(12l);
			f.setActivityDateTo(20l);
			f.setActivityDateInterval(PastIntervalValue.TEST);
			f.setContentType("ct");
			f.setDcpContentProvider("cp");

			tested.patchQuerySettings(qs);

			// assert patched fields
			Assert.assertEquals(0, qs.getFacets().size());
			Assert.assertEquals(false, qs.isQueryHighlight());
			Assert.assertEquals(new Integer(0), qs.getFilters().getFrom());
			Assert.assertEquals(new Integer(20), qs.getFilters().getSize());
			Assert.assertEquals(SortByValue.NEW, qs.getSortBy());
			Assert.assertEquals(9, qs.getFields().size());
			Assert.assertNull(qs.getFilters().getActivityDateFrom());
			Assert.assertNull(qs.getFilters().getActivityDateTo());
			Assert.assertNull(qs.getFilters().getActivityDateInterval());

			// assert preserved fields
			Assert.assertEquals("Querry", qs.getQuery());
			Assert.assertEquals(0, qs.getFacets().size());
			Assert.assertEquals("ct", qs.getFilters().getContentType());
			Assert.assertEquals("cp", qs.getFilters().getDcpContentProvider());

		}

		// case - SortByValue.NEW_CREATION allowed in QuerySettings
		{
			QuerySettings qs = new QuerySettings();
			qs.setSortBy(SortByValue.NEW_CREATION);

			tested.patchQuerySettings(qs);

			Assert.assertEquals(SortByValue.NEW_CREATION, qs.getSortBy());

		}

	}

	@Test
	public void feed() throws IOException {
		FeedRestService tested = getTested();
		tested.querySettingsParser = Mockito.mock(QuerySettingsParser.class);
		tested.searchService = Mockito.mock(SearchService.class);

		// case - incorrect input
		{
			Object response = tested.feed(null);
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
			SearchHits searchHits = Mockito.mock(SearchHits.class);
			Mockito.when(sr.hits()).thenReturn(searchHits);
			Mockito.when(
					tested.searchService.performSearch(Mockito.eq(qs), Mockito.notNull(String.class),
							Mockito.eq(StatsRecordType.FEED))).thenReturn(sr);
			Object response = tested.feed(uriInfo);
			Mockito.verify(uriInfo).getQueryParameters();
			Mockito.verify(tested.querySettingsParser).parseUriParams(qp);
			Mockito.verify(tested.searchService).performSearch(Mockito.eq(qs), Mockito.notNull(String.class),
					Mockito.eq(StatsRecordType.FEED));
			Assert.assertTrue("Bad class instead of Feed: " + response.getClass().getName(), response instanceof Feed);
		}

		// case - error handling for invalid request value
		{
			Mockito.reset(tested.querySettingsParser, tested.searchService);
			UriInfo uriInfo = Mockito.mock(UriInfo.class);
			MultivaluedMap<String, String> qp = new MultivaluedMapImpl<String, String>();
			Mockito.when(uriInfo.getQueryParameters()).thenReturn(qp);
			Mockito.when(tested.querySettingsParser.parseUriParams(qp)).thenThrow(
					new IllegalArgumentException("test exception"));
			Object response = tested.feed(uriInfo);
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
			Mockito.when(
					tested.searchService.performSearch(Mockito.eq(qs), Mockito.notNull(String.class),
							Mockito.eq(StatsRecordType.FEED))).thenThrow(new IndexMissingException(null));
			Object response = tested.feed(uriInfo);
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
			Mockito.when(
					tested.searchService.performSearch(Mockito.eq(qs), Mockito.notNull(String.class),
							Mockito.eq(StatsRecordType.FEED))).thenThrow(new RuntimeException());
			Object response = tested.feed(uriInfo);
			TestUtils.assertResponseStatus(response, Status.INTERNAL_SERVER_ERROR);
		}
	}

	// TODO UNITTEST FEED test creation of feed data from real ES JSON data

	@Test
	public void constructFeedTitle() throws URISyntaxException {
		FeedRestService tested = getTested();

		{
			QuerySettings querySettings = new QuerySettings();
			Assert.assertEquals("DCP whole content feed", tested.constructFeedTitle(querySettings).toString());
		}

		// case - string param
		{
			QuerySettings querySettings = new QuerySettings();
			querySettings.getFiltersInit().setContentType("conetnt_type");
			Assert.assertEquals("DCP content feed for criteria type=conetnt_type", tested.constructFeedTitle(querySettings)
					.toString());
		}

		// case - list param
		{
			QuerySettings querySettings = new QuerySettings();
			querySettings.getFiltersInit().addProject("as7");
			Assert.assertEquals("DCP content feed for criteria project=[as7]", tested.constructFeedTitle(querySettings)
					.toString());
			querySettings.getFiltersInit().addProject("aerogear");
			Assert.assertEquals("DCP content feed for criteria project=[as7, aerogear]",
					tested.constructFeedTitle(querySettings).toString());
		}

		// case - multiple params
		{
			QuerySettings querySettings = new QuerySettings();
			querySettings.getFiltersInit().addProject("as7");
			querySettings.getFiltersInit().addProject("aerogear");
			querySettings.getFiltersInit().addTag("tag1");
			querySettings.getFiltersInit().addTag("tag2");
			querySettings.getFiltersInit().setContentType("content_type");
			querySettings.getFiltersInit().setDcpContentProvider("jbossorg");
			querySettings.getFiltersInit().addDcpType("issue");
			querySettings.getFiltersInit().addDcpType("blogpost");
			querySettings.getFiltersInit().addContributor("John Doe <john@doe.org>");
			querySettings.setQuery("querry me fulltext");
			querySettings.setSortBy(SortByValue.NEW_CREATION);
			Assert
					.assertEquals(
							"DCP content feed for criteria project=[as7, aerogear] and contributor=[John Doe <john@doe.org>] and tag=[tag1, tag2] and dcp_type=[issue, blogpost] and type=content_type and content_provider=jbossorg and query='querry me fulltext' and sortBy=new-create",
							tested.constructFeedTitle(querySettings).toString());
		}

	}

	/**
	 * @return
	 */
	private FeedRestService getTested() {
		FeedRestService tested = new FeedRestService();
		tested.log = Logger.getLogger("test logger");
		return tested;
	}

}
