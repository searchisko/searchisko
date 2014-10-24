/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.internal.InternalSearchHitField;
import org.jboss.resteasy.plugins.providers.atom.Entry;
import org.jboss.resteasy.plugins.providers.atom.Feed;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.searchisko.api.ContentObjectFields;
import org.searchisko.api.model.PastIntervalValue;
import org.searchisko.api.model.QuerySettings;
import org.searchisko.api.model.QuerySettings.Filters;
import org.searchisko.api.model.SortByValue;
import org.searchisko.api.rest.exception.BadFieldException;
import org.searchisko.api.service.SearchService;
import org.searchisko.api.service.StatsRecordType;
import org.searchisko.api.service.SystemInfoService;
import org.searchisko.api.testtools.TestUtils;
import org.searchisko.api.util.QuerySettingsParser;
import org.searchisko.api.util.SearchUtils;

/**
 * Unit test for {@link FeedRestService}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class FeedRestServiceTest {

	final static private String CONTENT_TYPE_KEY = "type";
	final static private String SYS_TYPES_KEY = "sys_type";
	final static private String SYS_CONTENT_PROVIDER = "content_provider";
	final static private String PROJECTS_KEY = "project";
	final static private String TAGS_KEY = "tag";
	final static private String CONTRIBUTORS_KEY = "contributor";
	final static private String ACTIVITY_DATE_INTERVAL_KEY = "activity_date_interval";
	final static private String ACTIVITY_DATE_FROM_KEY = "activity_date_from";
	final static private String ACTIVITY_DATE_TO_KEY = "activity_date_to";

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
			Assert.assertEquals(new Integer(0), qs.getFrom());
			Assert.assertEquals(new Integer(20), qs.getSize());
			Assert.assertEquals(SortByValue.NEW, qs.getSortBy());
			Assert.assertEquals(9, qs.getFields().size());
			Assert.assertNull(qs.getFilters().getFilterCandidateValues(ACTIVITY_DATE_FROM_KEY));
			Assert.assertNull(qs.getFilters().getFilterCandidateValues(ACTIVITY_DATE_TO_KEY));
			Assert.assertNull(qs.getFilters().getFilterCandidateValues(ACTIVITY_DATE_INTERVAL_KEY));

			// assert preserved fields
			Assert.assertEquals(null, qs.getAggregations());
			Assert.assertNull(qs.getFilters().getFilterCandidateValues(CONTENT_TYPE_KEY));
			Assert.assertNull(qs.getFilters().getFilterCandidateValues(SYS_CONTENT_PROVIDER));
			Assert.assertNull(qs.getFilters().getFilterCandidateValues(CONTRIBUTORS_KEY));
			Assert.assertNull(qs.getFilters().getFilterCandidateValues(SYS_TYPES_KEY));
			Assert.assertNull(qs.getFilters().getFilterCandidateValues(PROJECTS_KEY));
			Assert.assertNull(qs.getFilters().getFilterCandidateValues(TAGS_KEY));
		}

		// case - set and reset some values on nonempty QuerySettings
		{
			QuerySettings qs = new QuerySettings();
			qs.addAggregation("testagg");
			qs.addField("field1");
			qs.addField("field2");
			qs.setQueryHighlight(true);
			qs.setQuery("Querry");
			// unsupported sort by must be changed
			qs.setSortBy(SortByValue.OLD);
			qs.setFrom(50);
			qs.setSize(150);
			Filters f = qs.getFiltersInit();
			f.acknowledgeUrlFilterCandidate(ACTIVITY_DATE_FROM_KEY, "12l");
			f.acknowledgeUrlFilterCandidate(ACTIVITY_DATE_TO_KEY, "20l");
			f.acknowledgeUrlFilterCandidate(ACTIVITY_DATE_INTERVAL_KEY, PastIntervalValue.TEST.toString());
			f.acknowledgeUrlFilterCandidate(CONTENT_TYPE_KEY, "ct");
			f.acknowledgeUrlFilterCandidate(SYS_CONTENT_PROVIDER, "cp");

			tested.patchQuerySettings(qs);

			// assert patched fields
			Assert.assertEquals(0, qs.getAggregations().size());
			Assert.assertEquals(false, qs.isQueryHighlight());
			Assert.assertEquals(SortByValue.NEW, qs.getSortBy());
			Assert.assertEquals(9, qs.getFields().size());
			Assert.assertNull(qs.getFilters().getFilterCandidateValues(ACTIVITY_DATE_FROM_KEY));
			Assert.assertNull(qs.getFilters().getFilterCandidateValues(ACTIVITY_DATE_TO_KEY));
			Assert.assertNull(qs.getFilters().getFilterCandidateValues(ACTIVITY_DATE_INTERVAL_KEY));

			// assert preserved fields
			Assert.assertEquals(new Integer(50), qs.getFrom());
			Assert.assertEquals(new Integer(150), qs.getSize());
			Assert.assertEquals("Querry", qs.getQuery());
			Assert.assertEquals(0, qs.getAggregations().size());
			Assert.assertEquals(1, qs.getFilters().getFilterCandidateValues(CONTENT_TYPE_KEY).size());
			Assert.assertEquals("ct", qs.getFilters().getFilterCandidateValues(CONTENT_TYPE_KEY).get(0));
			Assert.assertEquals(1, qs.getFilters().getFilterCandidateValues(SYS_CONTENT_PROVIDER).size());
			Assert.assertEquals("cp", qs.getFilters().getFilterCandidateValues(SYS_CONTENT_PROVIDER).get(0));

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
	public void feed() throws IOException, URISyntaxException {
		FeedRestService tested = getTested();
		tested.querySettingsParser = Mockito.mock(QuerySettingsParser.class);
		tested.searchService = Mockito.mock(SearchService.class);
		tested.systemInfoService = Mockito.mock(SystemInfoService.class);

		// case - correct processing sequence, null search hits, feed title without params
		{
			Mockito.reset(tested.querySettingsParser, tested.searchService);
			UriInfo uriInfo = Mockito.mock(UriInfo.class);
			MultivaluedMap<String, String> qp = new MultivaluedMapImpl<String, String>();
			Mockito.when(uriInfo.getQueryParameters()).thenReturn(qp);
			QuerySettings qs = new QuerySettings();
			Mockito.when(tested.querySettingsParser.parseUriParams(qp)).thenReturn(qs);
			prepareSearchResponseMocks(tested, qs, null);

			Object response = tested.feed(uriInfo);

			feedBasicAsserts(tested, uriInfo, qp, qs, response);

			Feed feed = (Feed) response;
			Assert.assertEquals(0, feed.getEntries().size());
			Assert.assertEquals("Whole feed content", feed.getTitle());
			Assert.assertNotNull(feed.getUpdated());
			Assert.assertNotNull(feed.getGenerator());
		}

		// case - correct processing sequence, empty search hits, feed title passed from outside
		{
			Mockito.reset(tested.querySettingsParser, tested.searchService);
			UriInfo uriInfo = Mockito.mock(UriInfo.class);
			MultivaluedMap<String, String> qp = new MultivaluedMapImpl<String, String>();
			qp.putSingle(FeedRestService.REQPARAM_FEED_TITLE, "test feed title");
			Mockito.when(uriInfo.getQueryParameters()).thenReturn(qp);
			QuerySettings qs = new QuerySettings();
			Mockito.when(tested.querySettingsParser.parseUriParams(qp)).thenReturn(qs);
			SearchHit[] ha = new SearchHit[0];
			prepareSearchResponseMocks(tested, qs, ha);

			Object response = tested.feed(uriInfo);

			feedBasicAsserts(tested, uriInfo, qp, qs, response);

			Feed feed = (Feed) response;
			Assert.assertEquals(0, feed.getEntries().size());
			Assert.assertEquals("test feed title", feed.getTitle());
			Assert.assertNotNull(feed.getUpdated());
			Assert.assertNotNull(feed.getGenerator());
		}

		// case - correct processing sequence with hits, feed title from param
		{
			Mockito.reset(tested.querySettingsParser, tested.searchService);
			UriInfo uriInfo = Mockito.mock(UriInfo.class);
			MultivaluedMap<String, String> qp = new MultivaluedMapImpl<String, String>();
			Mockito.when(uriInfo.getQueryParameters()).thenReturn(qp);
			QuerySettings qs = new QuerySettings();
			qs.getFiltersInit().acknowledgeUrlFilterCandidate(PROJECTS_KEY, "as7");
			Mockito.when(tested.querySettingsParser.parseUriParams(qp)).thenReturn(qs);

			// hit 1 - only sys_description, no sys_content
			SearchHit hit1 = Mockito.mock(SearchHit.class);
			Mockito.when(hit1.getId()).thenReturn("hit1");
			Map<String, SearchHitField> fields = new HashMap<String, SearchHitField>();
			Mockito.when(hit1.getFields()).thenReturn(fields);
			putSearchHitField(fields, ContentObjectFields.SYS_TITLE, "My title");
			putSearchHitField(fields, ContentObjectFields.SYS_URL_VIEW, "http://url1");
			putSearchHitField(fields, ContentObjectFields.SYS_DESCRIPTION, "My description");
			putSearchHitField(fields, ContentObjectFields.SYS_CREATED, "2012-11-02T12:55:44Z");
			putSearchHitField(fields, ContentObjectFields.SYS_LAST_ACTIVITY_DATE, "2012-11-01T12:55:44Z");
			putSearchHitField(fields, ContentObjectFields.SYS_CONTRIBUTORS, "John Doe <j@d.com>", "My Dear <my@de.org>");
			putSearchHitField(fields, ContentObjectFields.SYS_TAGS, "tag1", "tag2");

			// hit 2 - both sys_description and sys_content (with valid sys_content_content-type), invalid and empty date
			// fields, no
			// contributors nor tags
			SearchHit hit2 = Mockito.mock(SearchHit.class);
			Mockito.when(hit2.getId()).thenReturn("hit2");
			Map<String, SearchHitField> fields2 = new HashMap<String, SearchHitField>();
			Mockito.when(hit2.getFields()).thenReturn(fields2);
			putSearchHitField(fields2, ContentObjectFields.SYS_TITLE, "My title 2");
			putSearchHitField(fields2, ContentObjectFields.SYS_DESCRIPTION, "My description 2");
			putSearchHitField(fields2, ContentObjectFields.SYS_CONTENT, "My content 2");
			putSearchHitField(fields2, ContentObjectFields.SYS_CONTENT_CONTENT_TYPE, "text/html");
			putSearchHitField(fields2, ContentObjectFields.SYS_CREATED, "invaliddate");

			// hit 3 - sys_content (with invalid sys_content_content-type)
			SearchHit hit3 = Mockito.mock(SearchHit.class);
			Mockito.when(hit3.getId()).thenReturn("hit3");
			Map<String, SearchHitField> fields3 = new HashMap<String, SearchHitField>();
			Mockito.when(hit3.getFields()).thenReturn(fields3);
			putSearchHitField(fields3, ContentObjectFields.SYS_TITLE, "My title 3");
			putSearchHitField(fields3, ContentObjectFields.SYS_CONTENT, "My content 3");
			putSearchHitField(fields3, ContentObjectFields.SYS_CONTENT_CONTENT_TYPE, "bleble");

			SearchHit[] ha = new SearchHit[] { hit1, hit2, hit3 };
			prepareSearchResponseMocks(tested, qs, ha);

			Object response = tested.feed(uriInfo);

			feedBasicAsserts(tested, uriInfo, qp, qs, response);

			Feed feed = (Feed) response;
			Assert.assertEquals("Feed content for criteria project=[as7]", feed.getTitle());
			Assert.assertNotNull(feed.getUpdated());
			Assert.assertNotNull(feed.getGenerator());

			Assert.assertEquals(3, feed.getEntries().size());
			{
				Entry entry = feed.getEntries().get(0);
				Assert.assertEquals("searchisko:content:id:hit1", entry.getId().toString());
				Assert.assertEquals("My title", entry.getTitle());
				Assert.assertEquals(1, entry.getLinks().size());
				Assert.assertEquals(SearchUtils.dateFromISOString("2012-11-02T12:55:44Z", false), entry.getPublished());
				Assert.assertEquals(SearchUtils.dateFromISOString("2012-11-01T12:55:44Z", false), entry.getUpdated());
				Assert.assertEquals("http://url1", entry.getLinks().get(0).getHref().toString());
				Assert.assertNull(entry.getSummary());
				Assert.assertEquals("My description", entry.getContent().getText());
				Assert.assertEquals(MediaType.TEXT_PLAIN_TYPE, entry.getContent().getType());
				Assert.assertEquals(2, entry.getAuthors().size());
				Assert.assertEquals("John Doe", entry.getAuthors().get(0).getName());
				Assert.assertEquals("My Dear", entry.getAuthors().get(1).getName());
				// no emails in feed !
				Assert.assertNull(entry.getAuthors().get(0).getEmail());
				Assert.assertNull(entry.getAuthors().get(1).getEmail());
				Assert.assertEquals(0, entry.getContributors().size());
				Assert.assertEquals(2, entry.getCategories().size());
				Assert.assertEquals("tag1", entry.getCategories().get(0).getTerm());
				Assert.assertEquals("tag2", entry.getCategories().get(1).getTerm());
			}

			{
				Entry entry = feed.getEntries().get(1);
				Assert.assertEquals("searchisko:content:id:hit2", entry.getId().toString());
				Assert.assertEquals("My title 2", entry.getTitle());
				Assert.assertEquals(0, entry.getLinks().size());
				Assert.assertNull(entry.getPublished());
				Assert.assertNull(entry.getUpdated());
				Assert.assertEquals("My description 2", entry.getSummary());
				Assert.assertEquals("My content 2", entry.getContent().getText());
				Assert.assertEquals(MediaType.TEXT_HTML_TYPE, entry.getContent().getType());
				// one author added because ATOM spec requires it!
				Assert.assertEquals(1, entry.getAuthors().size());
				Assert.assertEquals("unknown", entry.getAuthors().get(0).getName());
				Assert.assertNull(entry.getAuthors().get(0).getEmail());
				Assert.assertEquals(0, entry.getContributors().size());
				Assert.assertEquals(0, entry.getCategories().size());
			}

			{
				Entry entry = feed.getEntries().get(2);
				Assert.assertEquals("searchisko:content:id:hit3", entry.getId().toString());
				Assert.assertEquals("My title 3", entry.getTitle());
				Assert.assertEquals(0, entry.getLinks().size());
				Assert.assertNull(entry.getPublished());
				Assert.assertNull(entry.getUpdated());
				Assert.assertNull(entry.getSummary());
				Assert.assertEquals("My content 3", entry.getContent().getText());
				// default type used here because invalid in input
				Assert.assertEquals(MediaType.TEXT_PLAIN_TYPE, entry.getContent().getType());
				// one author added because ATOM spec requires it!
				Assert.assertEquals(1, entry.getAuthors().size());
				Assert.assertEquals("unknown", entry.getAuthors().get(0).getName());
				Assert.assertNull(entry.getAuthors().get(0).getEmail());
				Assert.assertEquals(0, entry.getContributors().size());
				Assert.assertEquals(0, entry.getCategories().size());
			}

		}

	}

	private void putSearchHitField(Map<String, SearchHitField> fields, String name, Object... values) {
		fields.put(name, new InternalSearchHitField(name, Arrays.asList(values)));
	}

	private void feedBasicAsserts(FeedRestService tested, UriInfo uriInfo, MultivaluedMap<String, String> qp,
			QuerySettings qs, Object response) {
		Mockito.verify(uriInfo, Mockito.times(2)).getQueryParameters();
		Mockito.verify(tested.querySettingsParser).parseUriParams(qp);
		Mockito.verify(tested.searchService).performSearch(Mockito.eq(qs), Mockito.notNull(String.class),
				Mockito.eq(StatsRecordType.FEED));
		Assert.assertTrue("Bad class instead of Feed: " + response.getClass().getName(), response instanceof Feed);
	}

	@Test(expected = BadFieldException.class)
	public void feed_errorhandling_1() throws IOException, URISyntaxException {
		FeedRestService tested = getTested();

		tested.feed(null);
	}

	@Test(expected = BadFieldException.class)
	public void feed_errorhandling_2() throws IOException, URISyntaxException {
		FeedRestService tested = getTested();

		// case - error handling for invalid request value
		{
			Mockito.reset(tested.querySettingsParser, tested.searchService);
			UriInfo uriInfo = Mockito.mock(UriInfo.class);
			MultivaluedMap<String, String> qp = new MultivaluedMapImpl<String, String>();
			Mockito.when(uriInfo.getQueryParameters()).thenReturn(qp);
			Mockito.when(tested.querySettingsParser.parseUriParams(qp)).thenThrow(
					new IllegalArgumentException("test exception"));
			tested.feed(uriInfo);
		}
	}

	@Test
	public void feed_errorhandling_3() throws IOException, URISyntaxException {
		FeedRestService tested = getTested();

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
	}

	@Test(expected = RuntimeException.class)
	public void feed_errorhandling_4() throws IOException, URISyntaxException {
		FeedRestService tested = getTested();

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
			tested.feed(uriInfo);
		}
	}

	private void prepareSearchResponseMocks(FeedRestService tested, QuerySettings qs, SearchHit[] hitsArray) {
		SearchResponse sr = Mockito.mock(SearchResponse.class);
		SearchHits searchHits = Mockito.mock(SearchHits.class);
		Mockito.when(searchHits.getHits()).thenReturn(hitsArray);
		Mockito.when(sr.getHits()).thenReturn(searchHits);
		Mockito.when(
				tested.searchService.performSearch(Mockito.eq(qs), Mockito.notNull(String.class),
						Mockito.eq(StatsRecordType.FEED))).thenReturn(sr);
	}

	@Test
	public void constructFeedTitle() throws URISyntaxException {
		FeedRestService tested = getTested();

		{
			QuerySettings querySettings = new QuerySettings();
			Assert.assertEquals("Whole feed content", tested.constructFeedTitle(querySettings).toString());
		}

		// case - string param
		{
			QuerySettings querySettings = new QuerySettings();
			querySettings.getFiltersInit().acknowledgeUrlFilterCandidate(CONTENT_TYPE_KEY, "content_type");
			Assert.assertEquals("Feed content for criteria type=[content_type]", tested.constructFeedTitle(querySettings)
					.toString());
		}

		// case - list param
		{
			QuerySettings querySettings = new QuerySettings();
			querySettings.getFiltersInit().acknowledgeUrlFilterCandidate(PROJECTS_KEY, "as7");
			Assert.assertEquals("Feed content for criteria project=[as7]", tested.constructFeedTitle(querySettings)
					.toString());
			querySettings.getFiltersInit().acknowledgeUrlFilterCandidate(PROJECTS_KEY, "as7", "aerogear");
			Assert.assertEquals("Feed content for criteria project=[as7, aerogear]", tested.constructFeedTitle(querySettings)
					.toString());
		}

		// case - multiple params
		{
			QuerySettings querySettings = new QuerySettings();
			querySettings.getFiltersInit().acknowledgeUrlFilterCandidate(PROJECTS_KEY, "as7", "aerogear");
			querySettings.getFiltersInit().acknowledgeUrlFilterCandidate(TAGS_KEY, "tag1", "tag2");
			querySettings.getFiltersInit().acknowledgeUrlFilterCandidate(CONTENT_TYPE_KEY, "content_type");
			querySettings.getFiltersInit().acknowledgeUrlFilterCandidate(SYS_CONTENT_PROVIDER, "jbossorg");
			querySettings.getFiltersInit().acknowledgeUrlFilterCandidate(SYS_TYPES_KEY, "issue", "blogpost");
			querySettings.getFiltersInit().acknowledgeUrlFilterCandidate(CONTRIBUTORS_KEY, "John Doe <john@doe.org>");
			querySettings.setQuery("querry me fulltext");
			querySettings.setSortBy(SortByValue.NEW_CREATION);
			Assert
					.assertEquals(
							"Feed content for criteria project=[as7, aerogear] and contributor=[John Doe <john@doe.org>] and tag=[tag1, tag2] and sys_type=[issue, blogpost] and type=[content_type] and content_provider=[jbossorg] and query='querry me fulltext' and sortBy=new-create",
							tested.constructFeedTitle(querySettings).toString());
		}

	}

	/**
	 * @return
	 */
	private FeedRestService getTested() {
		FeedRestService tested = new FeedRestService();
		tested.log = Logger.getLogger("test logger");
		tested.querySettingsParser = Mockito.mock(QuerySettingsParser.class);
		tested.searchService = Mockito.mock(SearchService.class);
		tested.systemInfoService = Mockito.mock(SystemInfoService.class);

		return tested;
	}

}
