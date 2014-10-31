/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.common.joda.time.DateTimeZone;
import org.elasticsearch.common.settings.SettingsException;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.searchisko.api.model.PastIntervalValue;
import org.searchisko.api.model.QuerySettings;
import org.searchisko.api.model.QuerySettings.Filters;
import org.searchisko.api.model.SortByValue;
import org.searchisko.api.rest.exception.BadFieldException;
import org.searchisko.api.rest.exception.NotAuthorizedException;
import org.searchisko.api.security.Role;
import org.searchisko.api.testtools.TestUtils;

/**
 * Unit test for {@link SearchService}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 * @author Lukas Vlcek
 */
public class SearchServiceTest extends SearchServiceTestBase {

	/**
	 * https://github.com/searchisko/searchisko/issues/79
	 */
	@Test
	public void missingFilterFieldsConfigDoesNotThrowNPE() throws ReflectiveOperationException {
		ConfigService configService = Mockito.mock(ConfigService.class);
		Mockito.when(configService.get(ConfigService.CFGNAME_SEARCH_FULLTEXT_FILTER_FIELDS)).thenReturn(null);

		SearchService tested = getTested(configService);

		Filters filters = new Filters();
		filters.acknowledgeUrlFilterCandidate("test", "value");

		tested.parsedFilterConfigService.prepareFiltersForRequest(filters);
	}

	@Test
	public void getSearchResponseAdditionalFields() throws ReflectiveOperationException {

		ConfigService configService = mockConfigurationService();

		SearchService tested = getTested(configService);

		try {
			tested.getIntervalValuesForDateHistogramAggregations(null);
			Assert.fail("NullPointerException expected");
		} catch (NullPointerException e) {
			// OK
		}

		QuerySettings qs = new QuerySettings();
		Map<String, String> ret = tested.getIntervalValuesForDateHistogramAggregations(qs);
		Assert.assertTrue(ret.isEmpty());

		qs.addAggregation("activity_dates_histogram");
		qs.getFiltersInit().acknowledgeUrlFilterCandidate("activity_date_interval", PastIntervalValue.QUARTER.toString());
		tested.parsedFilterConfigService.prepareFiltersForRequest(qs.getFilters());
		ret = tested.getIntervalValuesForDateHistogramAggregations(qs);
		Assert.assertEquals(1, ret.size());
		Assert.assertEquals("week", ret.get("activity_dates_histogram_interval"));

	}

	@Test
	public void prepareIndexNamesCacheKey() {

		Assert.assertEquals("_all||false", SearchService.prepareIndexNamesCacheKey(null, false));
		Assert.assertEquals("_all||true", SearchService.prepareIndexNamesCacheKey(null, true));
		Set<String> sysTypesRequested = new HashSet<>();
		Assert.assertEquals("_all||false", SearchService.prepareIndexNamesCacheKey(sysTypesRequested, false));
		Assert.assertEquals("_all||true", SearchService.prepareIndexNamesCacheKey(sysTypesRequested, true));

		sysTypesRequested.add("aaaa");
		Assert.assertEquals("aaaa||true", SearchService.prepareIndexNamesCacheKey(sysTypesRequested, true));
		Assert.assertEquals("aaaa||false", SearchService.prepareIndexNamesCacheKey(sysTypesRequested, false));

		sysTypesRequested.add("bb");
		Assert.assertEquals("aaaa|bb||true", SearchService.prepareIndexNamesCacheKey(sysTypesRequested, true));
		Assert.assertEquals("aaaa|bb||false", SearchService.prepareIndexNamesCacheKey(sysTypesRequested, false));

		// check ordering
		sysTypesRequested = new HashSet<>();
		sysTypesRequested.add("bb");
		sysTypesRequested.add("zzzzz");
		sysTypesRequested.add("aaaa");
		Assert.assertEquals("aaaa|bb|zzzzz||true", SearchService.prepareIndexNamesCacheKey(sysTypesRequested, true));
		Assert.assertEquals("aaaa|bb|zzzzz||false", SearchService.prepareIndexNamesCacheKey(sysTypesRequested, false));

	}

	@Test
	public void handleCommonFiltersSettings_moreFilters() throws IOException, JSONException, ReflectiveOperationException {
		ConfigService configService = mockConfigurationService();

		SearchService tested = getTested(configService);

		// case - no filters object exists
		{
			tested.parsedFilterConfigService.prepareFiltersForRequest(null);
			Assert.assertEquals(0, tested.parsedFilterConfigService.getSearchFiltersForRequest().size());
		}

		QuerySettings querySettings = new QuerySettings();
		tested.parsedFilterConfigService.prepareFiltersForRequest(querySettings.getFilters());

		// case - empty filters object exists
		{
			QueryBuilder qb = QueryBuilders.matchAllQuery();
			QueryBuilder qbRes = tested.applyCommonFilters(tested.parsedFilterConfigService.getSearchFiltersForRequest(), qb);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_match_all.json", qbRes.toString());
		}

		Filters filters = new Filters();
		querySettings.setFilters(filters);
		tested.parsedFilterConfigService.prepareFiltersForRequest(filters);
		// case - empty filters object
		{
			QueryBuilder qb = QueryBuilders.matchAllQuery();
			QueryBuilder qbRes = tested.applyCommonFilters(tested.parsedFilterConfigService.getSearchFiltersForRequest(), qb);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_match_all.json", qbRes.toString());
		}

		// case - all filters used
		{
			filters.acknowledgeUrlFilterCandidate("sys_type", "mySysType", "mySysType2");
			filters.acknowledgeUrlFilterCandidate("content_provider", "my_content_provider");
			filters.acknowledgeUrlFilterCandidate("tag", "tag1", "tag2");
			filters.acknowledgeUrlFilterCandidate("project", "pr1", "pr2");
			filters.acknowledgeUrlFilterCandidate("contributor", "John Doe <john@doe.com>", "Dan Boo <boo@boo.net>");
			// activityDateInterval not tested here because variable, see separate test
			filters.acknowledgeUrlFilterCandidate("activity_date_from",
					new DateTime(1359232356456L).toString(DATE_TIME_FORMATTER_UTC));
			filters.acknowledgeUrlFilterCandidate("activity_date_to",
					new DateTime(1359232366456L).toString(DATE_TIME_FORMATTER_UTC));
			tested.parsedFilterConfigService.prepareFiltersForRequest(filters);
			QueryBuilder qb = QueryBuilders.matchAllQuery();
			QueryBuilder qbRes = tested.applyCommonFilters(tested.parsedFilterConfigService.getSearchFiltersForRequest(), qb);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_filters_moreFilters.json", qbRes.toString());
		}
	}

	@Test
	public void handleCommonFiltersSettings_ActivityDateInterval() throws IOException, JSONException,
			ReflectiveOperationException {

		ConfigService configService = mockConfigurationService();

		SearchService tested = getTested(configService);

		QuerySettings querySettings = new QuerySettings();
		Filters filters = new Filters();
		querySettings.setFilters(filters);
		filters.acknowledgeUrlFilterCandidate("activity_date_interval", PastIntervalValue.TEST.toString());
		// set date_from and date_to to some values to test this is ignored if interval is used
		filters.acknowledgeUrlFilterCandidate("activity_date_to",
				new DateTime(1359232366456L).toString(DATE_TIME_FORMATTER_UTC));
		filters.acknowledgeUrlFilterCandidate("activity_date_from",
				new DateTime(1359232356456L).toString(DATE_TIME_FORMATTER_UTC));
		tested.parsedFilterConfigService.prepareFiltersForRequest(filters);
		{
			QueryBuilder qb = QueryBuilders.matchAllQuery();
			QueryBuilder qbRes = tested.applyCommonFilters(tested.parsedFilterConfigService.getSearchFiltersForRequest(), qb);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_filters_activityDateInterval.json", qbRes.toString());
		}
	}

	@Test
	public void handleCommonFiltersSettings_ActivityDateFromTo() throws IOException, JSONException,
			ReflectiveOperationException {
		ConfigService configService = mockConfigurationService();

		SearchService tested = getTested(configService);

		QuerySettings querySettings = new QuerySettings();
		Filters filters = new Filters();
		querySettings.setFilters(filters);

		// case - only from
		filters.acknowledgeUrlFilterCandidate("activity_date_from",
				new DateTime(1359232356456L).toString(DATE_TIME_FORMATTER_UTC));
		filters.acknowledgeUrlFilterCandidate("activity_date_to");
		tested.parsedFilterConfigService.prepareFiltersForRequest(filters);
		{
			QueryBuilder qb = QueryBuilders.matchAllQuery();
			QueryBuilder qbRes = tested.applyCommonFilters(tested.parsedFilterConfigService.getSearchFiltersForRequest(), qb);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_filters_activityDateFrom.json", qbRes.toString());
		}

		// case - only to
		filters.acknowledgeUrlFilterCandidate("activity_date_from");
		filters.acknowledgeUrlFilterCandidate("activity_date_to",
				new DateTime(1359232366456L).toString(DATE_TIME_FORMATTER_UTC));
		tested.parsedFilterConfigService.prepareFiltersForRequest(filters);
		{
			QueryBuilder qb = QueryBuilders.matchAllQuery();
			QueryBuilder qbRes = tested.applyCommonFilters(tested.parsedFilterConfigService.getSearchFiltersForRequest(), qb);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_filters_activityDateTo.json", qbRes.toString());
		}

		// case - both bounds
		filters.acknowledgeUrlFilterCandidate("activity_date_from",
				new DateTime(1359232356456L).toString(DATE_TIME_FORMATTER_UTC));
		filters.acknowledgeUrlFilterCandidate("activity_date_to",
				new DateTime(1359232366456L).toString(DATE_TIME_FORMATTER_UTC));
		tested.parsedFilterConfigService.prepareFiltersForRequest(filters);
		{
			QueryBuilder qb = QueryBuilders.matchAllQuery();
			QueryBuilder qbRes = tested.applyCommonFilters(tested.parsedFilterConfigService.getSearchFiltersForRequest(), qb);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_filters_activityDateFromTo.json", qbRes.toString());
		}
	}

	@Test
	public void handleCommonFiltersSettings_projects() throws IOException, JSONException, ReflectiveOperationException {

		ConfigService configService = mockConfigurationService();

		SearchService tested = getTested(configService);

		QuerySettings querySettings = new QuerySettings();
		Filters filters = new Filters();
		querySettings.setFilters(filters);
		tested.parsedFilterConfigService.prepareFiltersForRequest(filters);
		// case - list of projects is null
		{
			QueryBuilder qb = QueryBuilders.matchAllQuery();
			QueryBuilder qbRes = tested.applyCommonFilters(tested.parsedFilterConfigService.getSearchFiltersForRequest(), qb);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_match_all.json", qbRes.toString());
		}

		// case - list of projects is empty
		{
			filters.acknowledgeUrlFilterCandidate("project", Collections.<String> emptyList());
			tested.parsedFilterConfigService.prepareFiltersForRequest(filters);
			QueryBuilder qb = QueryBuilders.matchAllQuery();
			QueryBuilder qbRes = tested.applyCommonFilters(tested.parsedFilterConfigService.getSearchFiltersForRequest(), qb);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_match_all.json", qbRes.toString());
		}

		// case - one project
		{
			filters.acknowledgeUrlFilterCandidate("project", "pr1");
			tested.parsedFilterConfigService.prepareFiltersForRequest(filters);
			QueryBuilder qb = QueryBuilders.matchAllQuery();
			QueryBuilder qbRes = tested.applyCommonFilters(tested.parsedFilterConfigService.getSearchFiltersForRequest(), qb);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_filters_projects_one.json", qbRes.toString());
		}

		// case - more projects
		{
			filters.acknowledgeUrlFilterCandidate("project", "pr1", "pr2", "pr3", "pr4");
			tested.parsedFilterConfigService.prepareFiltersForRequest(filters);
			QueryBuilder qb = QueryBuilders.matchAllQuery();
			QueryBuilder qbRes = tested.applyCommonFilters(tested.parsedFilterConfigService.getSearchFiltersForRequest(), qb);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_filters_projects_more.json", qbRes.toString());
		}
	}

	@Test
	public void handleCommonFiltersSettings_tags() throws IOException, JSONException, ReflectiveOperationException {

		ConfigService configService = mockConfigurationService();

		SearchService tested = getTested(configService);

		QuerySettings querySettings = new QuerySettings();
		Filters filters = new Filters();
		querySettings.setFilters(filters);
		tested.parsedFilterConfigService.prepareFiltersForRequest(filters);
		// case - list of tags is null
		{
			QueryBuilder qb = QueryBuilders.matchAllQuery();
			QueryBuilder qbRes = tested.applyCommonFilters(tested.parsedFilterConfigService.getSearchFiltersForRequest(), qb);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_match_all.json", qbRes.toString());
		}

		// case - list of tags is empty
		{
			filters.acknowledgeUrlFilterCandidate("tag", Collections.<String> emptyList());
			tested.parsedFilterConfigService.prepareFiltersForRequest(filters);
			QueryBuilder qb = QueryBuilders.matchAllQuery();
			QueryBuilder qbRes = tested.applyCommonFilters(tested.parsedFilterConfigService.getSearchFiltersForRequest(), qb);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_match_all.json", qbRes.toString());
		}

		// case - one tag
		{
			filters.acknowledgeUrlFilterCandidate("tag", "tg1");
			tested.parsedFilterConfigService.prepareFiltersForRequest(filters);
			QueryBuilder qb = QueryBuilders.matchAllQuery();
			QueryBuilder qbRes = tested.applyCommonFilters(tested.parsedFilterConfigService.getSearchFiltersForRequest(), qb);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_filters_tags_one.json", qbRes.toString());
		}

		// case - more tags
		{
			filters.acknowledgeUrlFilterCandidate("tag", "tg1", "tg2", "tg3", "tg4");
			tested.parsedFilterConfigService.prepareFiltersForRequest(filters);
			QueryBuilder qb = QueryBuilders.matchAllQuery();
			QueryBuilder qbRes = tested.applyCommonFilters(tested.parsedFilterConfigService.getSearchFiltersForRequest(), qb);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_filters_tags_more.json", qbRes.toString());
		}

		// case - all UPPER CASED chars in tags values are converted to LOWER CASED
		{
			filters.acknowledgeUrlFilterCandidate("tag", "TG1", "Tg2", "tG3", "tg4");
			tested.parsedFilterConfigService.prepareFiltersForRequest(filters);
			QueryBuilder qb = QueryBuilders.matchAllQuery();
			QueryBuilder qbRes = tested.applyCommonFilters(tested.parsedFilterConfigService.getSearchFiltersForRequest(), qb);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_filters_tags_more.json", qbRes.toString());
		}
	}

	@Test
	public void handleCommonFiltersSettings_contributors() throws IOException, JSONException,
			ReflectiveOperationException {

		ConfigService configService = mockConfigurationService();

		SearchService tested = getTested(configService);

		QuerySettings querySettings = new QuerySettings();
		Filters filters = new Filters();
		querySettings.setFilters(filters);
		tested.parsedFilterConfigService.prepareFiltersForRequest(filters);
		// case - list of contributors is null
		{
			QueryBuilder qb = QueryBuilders.matchAllQuery();
			QueryBuilder qbRes = tested.applyCommonFilters(tested.parsedFilterConfigService.getSearchFiltersForRequest(), qb);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_match_all.json", qbRes.toString());
		}

		// case - list of contributors is empty
		{
			filters.acknowledgeUrlFilterCandidate("contributor", Collections.<String> emptyList());
			tested.parsedFilterConfigService.prepareFiltersForRequest(filters);
			QueryBuilder qb = QueryBuilders.matchAllQuery();
			QueryBuilder qbRes = tested.applyCommonFilters(tested.parsedFilterConfigService.getSearchFiltersForRequest(), qb);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_match_all.json", qbRes.toString());
		}

		// case - one contributor
		{
			filters.acknowledgeUrlFilterCandidate("contributor", "tg1");
			tested.parsedFilterConfigService.prepareFiltersForRequest(filters);
			QueryBuilder qb = QueryBuilders.matchAllQuery();
			QueryBuilder qbRes = tested.applyCommonFilters(tested.parsedFilterConfigService.getSearchFiltersForRequest(), qb);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_filters_contributors_one.json", qbRes.toString());
		}

		// case - more contributors
		{
			filters.acknowledgeUrlFilterCandidate("contributor", "tg1", "tg2", "tg3", "tg4");
			tested.parsedFilterConfigService.prepareFiltersForRequest(filters);
			QueryBuilder qb = QueryBuilders.matchAllQuery();
			QueryBuilder qbRes = tested.applyCommonFilters(tested.parsedFilterConfigService.getSearchFiltersForRequest(), qb);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_filters_contributors_more.json", qbRes.toString());
		}
	}

	@Test
	public void handleFulltextSearchSettings() throws IOException, JSONException {
		ConfigService configService = Mockito.mock(ConfigService.class);
		SearchService tested = getTested(configService);

		// case - NPE when no settings passed in
		try {
			tested.prepareQueryBuilder(null);
			Assert.fail("NullPointerException expected");
		} catch (NullPointerException e) {
			// OK
		}

		QuerySettings querySettings = new QuerySettings();

		// case - no fulltext parameter requested
		{
			querySettings.setQuery(null);
			QueryBuilder qbRes = tested.prepareQueryBuilder(querySettings);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_match_all.json", qbRes.toString());
		}

		// case - fulltext parameter requested, no fulltext fields configured
		{
			Mockito.reset(tested.configService);
			Mockito.when(tested.configService.get(ConfigService.CFGNAME_SEARCH_FULLTEXT_QUERY_FIELDS)).thenReturn(null);
			querySettings.setQuery("my query string");
			QueryBuilder qbRes = tested.prepareQueryBuilder(querySettings);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_fulltext.json", qbRes.toString());
			Mockito.verify(tested.configService).get(ConfigService.CFGNAME_SEARCH_FULLTEXT_QUERY_FIELDS);
			Mockito.verifyNoMoreInteractions(tested.configService);
		}

		// case - fulltext parameter requested, some fulltext fields configured (one with invalid format)
		{
			Mockito.reset(tested.configService);
			Mockito.when(tested.configService.get(ConfigService.CFGNAME_SEARCH_FULLTEXT_QUERY_FIELDS)).thenReturn(
					TestUtils.loadJSONFromClasspathFile("/search/search_fulltext_query_fields.json"));
			querySettings.setQuery("my query string");
			QueryBuilder qbRes = tested.prepareQueryBuilder(querySettings);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_fulltext_fields.json", qbRes.toString());
			Mockito.verify(tested.configService).get(ConfigService.CFGNAME_SEARCH_FULLTEXT_QUERY_FIELDS);
			Mockito.verifyNoMoreInteractions(tested.configService);
		}

		// case - no fulltext parameter requested, some fulltext fields configured which has no effect
		{
			querySettings.setQuery(null);
			Mockito.reset(tested.configService);
			Mockito.when(tested.configService.get(ConfigService.CFGNAME_SEARCH_FULLTEXT_QUERY_FIELDS)).thenReturn(
					TestUtils.loadJSONFromClasspathFile("/search/search_fulltext_query_fields.json"));
			QueryBuilder qbRes = tested.prepareQueryBuilder(querySettings);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_match_all.json", qbRes.toString());
			Mockito.verifyZeroInteractions(tested.configService);
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void handleHighlightSettings() {
		ConfigService configService = Mockito.mock(ConfigService.class);
		SearchService tested = getTested(configService);

		SearchRequestBuilder srbMock = Mockito.mock(SearchRequestBuilder.class);

		// case - NPE when no settings passed in
		try {
			tested.setSearchRequestHighlight(null, srbMock);
			Assert.fail("NullPointerException expected");
		} catch (NullPointerException e) {
			// OK
		}

		QuerySettings querySettings = new QuerySettings();

		// case - highlight requested but no fulltext query requested so nothing done
		{
			Mockito.reset(srbMock, tested.configService);
			querySettings.setQuery(null);
			querySettings.setQueryHighlight(true);
			tested.setSearchRequestHighlight(querySettings, srbMock);
			Mockito.verifyZeroInteractions(srbMock);
			Mockito.verifyZeroInteractions(tested.configService);
		}

		// case - highlight requested not requested, fulltext query requested, nothing done
		{
			Mockito.reset(srbMock, tested.configService);
			querySettings.setQuery("query");
			querySettings.setQueryHighlight(false);
			tested.setSearchRequestHighlight(querySettings, srbMock);
			Mockito.verifyZeroInteractions(srbMock);
			Mockito.verifyZeroInteractions(tested.configService);
		}

		// case - highlight and fulltext query requested, configuration OK
		{
			Mockito.reset(srbMock, tested.configService);
			querySettings.setQuery("query");
			querySettings.setQueryHighlight(true);
			Map<String, Object> cfg = TestUtils.loadJSONFromClasspathFile("/search/search_fulltext_highlight_fields.json");
			Mockito.when(tested.configService.get(ConfigService.CFGNAME_SEARCH_FULLTEXT_HIGHLIGHT_FIELDS)).thenReturn(cfg);
			tested.setSearchRequestHighlight(querySettings, srbMock);
			Mockito.verify(srbMock).setHighlighterPreTags("<span class='hlt'>");
			Mockito.verify(srbMock).setHighlighterPostTags("</span>");
			Mockito.verify(srbMock).setHighlighterEncoder("html");
			Mockito.verify(srbMock).addHighlightedField("sys_title", -1, 0, 0);
			Mockito.verify(srbMock).addHighlightedField("sys_description", 2, 3, 20);
			Mockito.verify(srbMock).addHighlightedField("sys_contributors.fulltext", 5, 10, 30);
			Mockito.verifyNoMoreInteractions(srbMock);
			Mockito.verify(tested.configService).get(ConfigService.CFGNAME_SEARCH_FULLTEXT_HIGHLIGHT_FIELDS);
			Mockito.verifyNoMoreInteractions(tested.configService);
		}

		// cases - highlight and fulltext query requested, distinct configuration errors
		{
			Mockito.reset(srbMock, tested.configService);
			querySettings.setQuery("query");
			querySettings.setQueryHighlight(true);
			Mockito.when(tested.configService.get(ConfigService.CFGNAME_SEARCH_FULLTEXT_HIGHLIGHT_FIELDS)).thenReturn(null);
			try {
				tested.setSearchRequestHighlight(querySettings, srbMock);
				Assert.fail("SettingsException expected");
			} catch (SettingsException e) {
				// OK
			}
		}
		{
			Mockito.reset(srbMock, tested.configService);
			querySettings.setQuery("query");
			querySettings.setQueryHighlight(true);
			Mockito.when(tested.configService.get(ConfigService.CFGNAME_SEARCH_FULLTEXT_HIGHLIGHT_FIELDS)).thenReturn(
					new HashMap<String, Object>());
			try {
				tested.setSearchRequestHighlight(querySettings, srbMock);
				Assert.fail("SettingsException expected");
			} catch (SettingsException e) {
				// OK
			}
		}
		{
			Mockito.reset(srbMock, tested.configService);
			querySettings.setQuery("query");
			querySettings.setQueryHighlight(true);
			Map<String, Object> cfg = TestUtils.loadJSONFromClasspathFile("/search/search_fulltext_highlight_fields.json");
			cfg.put("sys_title", "badclass");
			Mockito.when(tested.configService.get(ConfigService.CFGNAME_SEARCH_FULLTEXT_HIGHLIGHT_FIELDS)).thenReturn(cfg);
			try {
				tested.setSearchRequestHighlight(querySettings, srbMock);
				Assert.fail("SettingsException expected");
			} catch (SettingsException e) {
				// OK
			}
		}
		{
			Mockito.reset(srbMock, tested.configService);
			querySettings.setQuery("query");
			querySettings.setQueryHighlight(true);
			Map<String, Object> cfg = TestUtils.loadJSONFromClasspathFile("/search/search_fulltext_highlight_fields.json");
			Map<String, String> c = (Map<String, String>) cfg.get("sys_title");
			// no integer parameter
			c.put("fragment_size", "no integer");
			Mockito.when(tested.configService.get(ConfigService.CFGNAME_SEARCH_FULLTEXT_HIGHLIGHT_FIELDS)).thenReturn(cfg);
			try {
				tested.setSearchRequestHighlight(querySettings, srbMock);
				Assert.fail("SettingsException expected");
			} catch (SettingsException e) {
				// OK
			}
		}
		{
			Mockito.reset(srbMock, tested.configService);
			querySettings.setQuery("query");
			querySettings.setQueryHighlight(true);
			Map<String, Object> cfg = TestUtils.loadJSONFromClasspathFile("/search/search_fulltext_highlight_fields.json");
			Map<String, String> c = (Map<String, String>) cfg.get("sys_title");
			// empty parameter
			c.put("number_of_fragments", "");
			Mockito.when(tested.configService.get(ConfigService.CFGNAME_SEARCH_FULLTEXT_HIGHLIGHT_FIELDS)).thenReturn(cfg);
			try {
				tested.setSearchRequestHighlight(querySettings, srbMock);
				Assert.fail("SettingsException expected");
			} catch (SettingsException e) {
				// OK
			}
		}
		{
			Mockito.reset(srbMock, tested.configService);
			querySettings.setQuery("query");
			querySettings.setQueryHighlight(true);
			Map<String, Object> cfg = TestUtils.loadJSONFromClasspathFile("/search/search_fulltext_highlight_fields.json");
			Map<String, String> c = (Map<String, String>) cfg.get("sys_title");
			// no integer parameter
			c.put("fragment_offset", "no integer");
			Mockito.when(tested.configService.get(ConfigService.CFGNAME_SEARCH_FULLTEXT_HIGHLIGHT_FIELDS)).thenReturn(cfg);
			try {
				tested.setSearchRequestHighlight(querySettings, srbMock);
				Assert.fail("SettingsException expected");
			} catch (SettingsException e) {
				// OK
			}
		}
	}

	@Test
	public void handleResponseContentSettings_pager() {
		ConfigService configService = Mockito.mock(ConfigService.class);
		SearchService tested = getTested(configService);

		SearchRequestBuilder srbMock = Mockito.mock(SearchRequestBuilder.class);

		// case - NPE when no settings passed in
		try {
			tested.setSearchRequestFromSize(null, srbMock);
			Assert.fail("NullPointerException expected");
		} catch (NullPointerException e) {
			Mockito.verifyNoMoreInteractions(srbMock);
			// OK
		}

		// case - nothing requested
		{
			// no filters object
			Mockito.reset(srbMock);
			QuerySettings querySettings = new QuerySettings();
			tested.setSearchRequestFromSize(querySettings, srbMock);
			Mockito.verifyNoMoreInteractions(srbMock);

			// empty filters object
			Mockito.reset(srbMock);
			querySettings.setFilters(new Filters());
			tested.setSearchRequestFromSize(querySettings, srbMock);
			Mockito.verifyNoMoreInteractions(srbMock);
		}

		// case - size requested
		{
			Mockito.reset(srbMock);
			QuerySettings querySettings = new QuerySettings();
			querySettings.setFilters(new Filters());
			querySettings.setSize(124);
			tested.setSearchRequestFromSize(querySettings, srbMock);
			Mockito.verify(srbMock).setSize(124);
			Mockito.verifyNoMoreInteractions(srbMock);
		}

		// case - size requested but over maximum so stripped
		{
			Mockito.reset(srbMock);
			QuerySettings querySettings = new QuerySettings();
			querySettings.setFilters(new Filters());
			querySettings.setSize(SearchService.RESPONSE_MAX_SIZE + 10);
			tested.setSearchRequestFromSize(querySettings, srbMock);
			Mockito.verify(srbMock).setSize(SearchService.RESPONSE_MAX_SIZE);
			Mockito.verifyNoMoreInteractions(srbMock);
		}

		// case - size requested is 0
		{
			Mockito.reset(srbMock);
			QuerySettings querySettings = new QuerySettings();
			querySettings.setFilters(new Filters());
			querySettings.setSize(0);
			tested.setSearchRequestFromSize(querySettings, srbMock);
			Mockito.verify(srbMock).setSize(0);
			Mockito.verifyNoMoreInteractions(srbMock);
		}

		// case - size requested is under 0 so not used
		{
			Mockito.reset(srbMock);
			QuerySettings querySettings = new QuerySettings();
			querySettings.setFilters(new Filters());
			querySettings.setSize(-1);
			tested.setSearchRequestFromSize(querySettings, srbMock);
			Mockito.verifyNoMoreInteractions(srbMock);
		}

		// case - from requested
		{
			Mockito.reset(srbMock);
			QuerySettings querySettings = new QuerySettings();
			querySettings.setFilters(new Filters());
			querySettings.setFrom(42);
			tested.setSearchRequestFromSize(querySettings, srbMock);
			Mockito.verify(srbMock).setFrom(42);
			Mockito.verifyNoMoreInteractions(srbMock);
		}

		// case - from requested is 0
		{
			Mockito.reset(srbMock);
			QuerySettings querySettings = new QuerySettings();
			querySettings.setFilters(new Filters());
			querySettings.setFrom(0);
			tested.setSearchRequestFromSize(querySettings, srbMock);
			Mockito.verify(srbMock).setFrom(0);
			Mockito.verifyNoMoreInteractions(srbMock);
		}

		// case - from requested is under 0 so not used
		{
			Mockito.reset(srbMock);
			QuerySettings querySettings = new QuerySettings();
			querySettings.setFilters(new Filters());
			querySettings.setFrom(-1);
			tested.setSearchRequestFromSize(querySettings, srbMock);
			Mockito.verifyNoMoreInteractions(srbMock);
		}

		// case - size and from requested
		{
			Mockito.reset(srbMock);
			QuerySettings querySettings = new QuerySettings();
			querySettings.setFilters(new Filters());
			querySettings.setSize(124);
			querySettings.setFrom(42);
			tested.setSearchRequestFromSize(querySettings, srbMock);
			Mockito.verify(srbMock).setSize(124);
			Mockito.verify(srbMock).setFrom(42);
			Mockito.verifyNoMoreInteractions(srbMock);
		}
	}

	@Test
	public void handleResponseContentSettings_fields() {
		ConfigService configService = Mockito.mock(ConfigService.class);
		SearchService tested = getTested(configService);

		SearchRequestBuilder srbMock = Mockito.mock(SearchRequestBuilder.class);

		// case - NPE when no settings passed in
		try {
			tested.setSearchRequestFields(null, srbMock);
			Assert.fail("NullPointerException expected");
		} catch (NullPointerException e) {
			Mockito.verifyNoMoreInteractions(srbMock);
			// OK
		}

		// case - no fields requested so defaults loaded from configuration but null
		{
			Mockito.reset(srbMock, tested.configService);
			Mockito.when(tested.configService.get(ConfigService.CFGNAME_SEARCH_RESPONSE_FIELDS)).thenReturn(null);
			QuerySettings querySettings = new QuerySettings();
			tested.setSearchRequestFields(querySettings, srbMock);
			Mockito.verify(tested.configService).get(ConfigService.CFGNAME_SEARCH_RESPONSE_FIELDS);
			Mockito.verifyNoMoreInteractions(srbMock);
			Mockito.verifyNoMoreInteractions(tested.configService);
		}

		// case - no fields requested so defaults loaded from configuration but do not contains correct key
		{
			Mockito.reset(srbMock, tested.configService);
			Map<String, Object> mockConfig = new HashMap<>();
			Mockito.when(tested.configService.get(ConfigService.CFGNAME_SEARCH_RESPONSE_FIELDS)).thenReturn(mockConfig);
			QuerySettings querySettings = new QuerySettings();
			tested.setSearchRequestFields(querySettings, srbMock);
			Mockito.verify(tested.configService).get(ConfigService.CFGNAME_SEARCH_RESPONSE_FIELDS);
			Mockito.verifyNoMoreInteractions(srbMock);
			Mockito.verifyNoMoreInteractions(tested.configService);
		}

		// case - no fields requested so defaults loaded from configuration, contains correct key with String value
		{
			Mockito.reset(srbMock, tested.configService);
			Map<String, Object> mockConfig = new HashMap<>();
			mockConfig.put(ConfigService.CFGNAME_SEARCH_RESPONSE_FIELDS, "aa");
			Mockito.when(tested.configService.get(ConfigService.CFGNAME_SEARCH_RESPONSE_FIELDS)).thenReturn(mockConfig);
			QuerySettings querySettings = new QuerySettings();
			tested.setSearchRequestFields(querySettings, srbMock);
			Mockito.verify(tested.configService).get(ConfigService.CFGNAME_SEARCH_RESPONSE_FIELDS);
			Mockito.verify(srbMock).addFields(new String[] { "aa" });
			Mockito.verifyNoMoreInteractions(srbMock);
			Mockito.verifyNoMoreInteractions(tested.configService);
		}

		// case - no fields requested so defaults loaded from configuration, contains correct key with List value
		{
			Mockito.reset(srbMock, tested.configService);
			Map<String, Object> mockConfig = new HashMap<>();
			List<String> cfgList = new ArrayList<>();
			cfgList.add("bb");
			cfgList.add("cc");
			mockConfig.put(ConfigService.CFGNAME_SEARCH_RESPONSE_FIELDS, cfgList);
			Mockito.when(tested.configService.get(ConfigService.CFGNAME_SEARCH_RESPONSE_FIELDS)).thenReturn(mockConfig);
			QuerySettings querySettings = new QuerySettings();
			tested.setSearchRequestFields(querySettings, srbMock);
			Mockito.verify(tested.configService).get(ConfigService.CFGNAME_SEARCH_RESPONSE_FIELDS);
			Mockito.verify(srbMock).addFields("bb", "cc");
			Mockito.verifyNoMoreInteractions(srbMock);
			Mockito.verifyNoMoreInteractions(tested.configService);
		}

		// case - fields requested
		{
			Mockito.reset(srbMock, tested.configService);
			QuerySettings querySettings = new QuerySettings();
			querySettings.addField("aa");
			querySettings.addField("bb");
			tested.setSearchRequestFields(querySettings, srbMock);
			Mockito.verify(srbMock).addFields("aa", "bb");
			Mockito.verifyNoMoreInteractions(srbMock);
			Mockito.verify(tested.configService).get(ConfigService.CFGNAME_SEARCH_RESPONSE_FIELDS);
			Mockito.verifyZeroInteractions(tested.configService);
		}

		// case - fields requested but * used there which is invalid
		{
			Mockito.reset(srbMock, tested.configService);
			QuerySettings querySettings = new QuerySettings();
			querySettings.addField("aa");
			querySettings.addField("*");
			try {
				tested.setSearchRequestFields(querySettings, srbMock);
				Assert.fail("BadFieldException expected");
			} catch (BadFieldException e) {
				Assert.assertEquals(QuerySettings.FIELDS_KEY, e.getFieldName());

			}
		}
	}

	@Test
	public void handleResponseContentSettings_fields_security() {
		ConfigService configService = Mockito.mock(ConfigService.class);
		SearchService tested = getTested(configService);

		SearchRequestBuilder srbMock = Mockito.mock(SearchRequestBuilder.class);

		Map<String, Object> mockConfig = new HashMap<>();
		List<String> cfgList = new ArrayList<>();
		cfgList.add("bb");
		cfgList.add("cc");
		mockConfig.put(ConfigService.CFGNAME_SEARCH_RESPONSE_FIELDS, cfgList);

		Map<String, Object> rolesSettings = new HashMap<>();
		rolesSettings.put("aa", "role1");
		rolesSettings.put("bb", "role1");
		rolesSettings.put("cc", "role1");
		mockConfig.put(SearchService.CFGNAME_FIELD_VISIBLE_FOR_ROLES, rolesSettings);

		// we configure source filtering to check it is not used when _source field is not requested
		mockConfig.put(SearchService.CFGNAME_SOURCE_FILTERING_FOR_ROLES, rolesSettings);

		// case - no fields requested so defaults loaded from configuration, but no any available for current user
		{
			Mockito.reset(srbMock, tested.configService, tested.authenticationUtilService);
			mockAuthenticatedUserWithRole(tested, "role2");
			Mockito.when(tested.configService.get(ConfigService.CFGNAME_SEARCH_RESPONSE_FIELDS)).thenReturn(mockConfig);
			QuerySettings querySettings = new QuerySettings();

			try {
				tested.setSearchRequestFields(querySettings, srbMock);
				Assert.fail("NotAuthorizedException expected");
			} catch (NotAuthorizedException e) {
				Mockito.verify(tested.configService).get(ConfigService.CFGNAME_SEARCH_RESPONSE_FIELDS);
				Mockito.verifyNoMoreInteractions(srbMock);
				Mockito.verifyNoMoreInteractions(tested.configService);
			}
		}

		// case - no fields requested so defaults loaded from configuration, some available for current user
		rolesSettings.put("bb", TestUtils.createListOfStrings("role1", "role2"));
		{
			Mockito.reset(srbMock, tested.configService, tested.authenticationUtilService);
			mockAuthenticatedUserWithRole(tested, "role2");
			Mockito.when(tested.configService.get(ConfigService.CFGNAME_SEARCH_RESPONSE_FIELDS)).thenReturn(mockConfig);
			QuerySettings querySettings = new QuerySettings();
			tested.setSearchRequestFields(querySettings, srbMock);
			Mockito.verify(tested.configService).get(ConfigService.CFGNAME_SEARCH_RESPONSE_FIELDS);
			Mockito.verify(srbMock).addFields("bb");
			Mockito.verifyNoMoreInteractions(srbMock);
			Mockito.verifyNoMoreInteractions(tested.configService);
		}

		// case - no fields requested so defaults loaded from configuration, all available for admin role
		{
			Mockito.reset(srbMock, tested.configService, tested.authenticationUtilService);
			mockAuthenticatedUserWithRole(tested, Role.ADMIN);
			Mockito.when(tested.configService.get(ConfigService.CFGNAME_SEARCH_RESPONSE_FIELDS)).thenReturn(mockConfig);
			QuerySettings querySettings = new QuerySettings();
			tested.setSearchRequestFields(querySettings, srbMock);
			Mockito.verify(tested.configService).get(ConfigService.CFGNAME_SEARCH_RESPONSE_FIELDS);
			Mockito.verify(srbMock).addFields("bb", "cc");
			Mockito.verifyNoMoreInteractions(srbMock);
			Mockito.verifyNoMoreInteractions(tested.configService);
		}

		// case - fields requested, but no any available for current user
		{
			Mockito.reset(srbMock, tested.configService, tested.authenticationUtilService);
			mockAuthenticatedUserWithRole(tested, "role2");
			Mockito.when(tested.configService.get(ConfigService.CFGNAME_SEARCH_RESPONSE_FIELDS)).thenReturn(mockConfig);
			QuerySettings querySettings = new QuerySettings();
			querySettings.addField("aa");
			querySettings.addField("cc");
			try {
				tested.setSearchRequestFields(querySettings, srbMock);
				Assert.fail("NotAuthorizedException expected");
			} catch (NotAuthorizedException e) {
				Mockito.verify(tested.configService).get(ConfigService.CFGNAME_SEARCH_RESPONSE_FIELDS);
				Mockito.verifyNoMoreInteractions(srbMock);
				Mockito.verifyNoMoreInteractions(tested.configService);
			}
		}

		// case - fields requested, some available for current user
		{
			Mockito.reset(srbMock, tested.configService, tested.authenticationUtilService);
			mockAuthenticatedUserWithRole(tested, "role1");
			Mockito.when(tested.configService.get(ConfigService.CFGNAME_SEARCH_RESPONSE_FIELDS)).thenReturn(mockConfig);
			QuerySettings querySettings = new QuerySettings();
			querySettings.addField("aa");
			querySettings.addField("bb");
			tested.setSearchRequestFields(querySettings, srbMock);
			Mockito.verify(tested.configService).get(ConfigService.CFGNAME_SEARCH_RESPONSE_FIELDS);
			Mockito.verify(srbMock).addFields("aa", "bb");
			Mockito.verifyNoMoreInteractions(srbMock);
			Mockito.verifyNoMoreInteractions(tested.configService);
		}

		// case - fields requested, all available for admin role
		{
			Mockito.reset(srbMock, tested.configService, tested.authenticationUtilService);
			mockAuthenticatedUserWithRole(tested, Role.ADMIN);
			Mockito.when(tested.configService.get(ConfigService.CFGNAME_SEARCH_RESPONSE_FIELDS)).thenReturn(mockConfig);
			QuerySettings querySettings = new QuerySettings();
			querySettings.addField("aa");
			querySettings.addField("bb");
			querySettings.addField("cc");
			tested.setSearchRequestFields(querySettings, srbMock);
			Mockito.verify(tested.configService).get(ConfigService.CFGNAME_SEARCH_RESPONSE_FIELDS);
			Mockito.verify(srbMock).addFields("aa", "bb", "cc");
			Mockito.verifyNoMoreInteractions(srbMock);
			Mockito.verifyNoMoreInteractions(tested.configService);
		}

	}

	@Test
	public void handleResponseContentSettings_fields_source_filtering() {
		// note that test which covers source filtering is not used when _source is not requested is in
		// handleResponseContentSettings_fields_security()

		ConfigService configService = Mockito.mock(ConfigService.class);
		SearchService tested = getTested(configService);

		SearchRequestBuilder srbMock = Mockito.mock(SearchRequestBuilder.class);

		Map<String, Object> mockConfig = new HashMap<>();

		Map<String, Object> rolesSettings = new HashMap<>();
		rolesSettings.put("*.aa", "role1");
		rolesSettings.put("bb", "role1");
		rolesSettings.put("cc.*", TestUtils.createListOfStrings("role1", "role2"));
		rolesSettings.put("dd", "role2");
		mockConfig.put(SearchService.CFGNAME_SOURCE_FILTERING_FOR_ROLES, rolesSettings);

		QuerySettings querySettings = new QuerySettings();

		// case - check source filtering is applied if not any field is requested, as elasticsearch returns source in
		// this case
		{
			Mockito.reset(srbMock, tested.configService, tested.authenticationUtilService);
			Mockito.when(tested.configService.get(ConfigService.CFGNAME_SEARCH_RESPONSE_FIELDS)).thenReturn(mockConfig);
			Mockito.when(srbMock.setFetchSource(Mockito.any(String[].class), Mockito.any(String[].class))).thenAnswer(
					new SourceExcludeMatcher(srbMock, TestUtils.createListOfStrings("*.aa", "bb", "cc.*", "dd")));

			tested.setSearchRequestFields(querySettings, srbMock);
			Mockito.verify(tested.configService).get(ConfigService.CFGNAME_SEARCH_RESPONSE_FIELDS);
			Mockito.verify(srbMock).setFetchSource(Mockito.any(String[].class), Mockito.any(String[].class));
			Mockito.verifyNoMoreInteractions(srbMock);
			Mockito.verifyNoMoreInteractions(tested.configService);
		}

		// set _source field as requested for other tests
		querySettings.addField("_source");

		// case - source filtering not applied when not configured
		{
			Mockito.reset(srbMock, tested.configService, tested.authenticationUtilService);
			mockAuthenticatedUserWithRole(tested, "role1");
			Mockito.when(tested.configService.get(ConfigService.CFGNAME_SEARCH_RESPONSE_FIELDS)).thenReturn(
					new HashMap<String, Object>());

			tested.setSearchRequestFields(querySettings, srbMock);
			Mockito.verify(tested.configService).get(ConfigService.CFGNAME_SEARCH_RESPONSE_FIELDS);
			Mockito.verify(srbMock).addFields("_source");
			Mockito.verifyNoMoreInteractions(srbMock);
			Mockito.verifyNoMoreInteractions(tested.configService);
		}

		// case - filtering configured, anonymous user has filtered source
		{
			Mockito.reset(srbMock, tested.configService, tested.authenticationUtilService);
			Mockito.when(tested.configService.get(ConfigService.CFGNAME_SEARCH_RESPONSE_FIELDS)).thenReturn(mockConfig);
			Mockito.when(srbMock.setFetchSource(Mockito.any(String[].class), Mockito.any(String[].class))).thenAnswer(
					new SourceExcludeMatcher(srbMock, TestUtils.createListOfStrings("*.aa", "bb", "cc.*", "dd")));

			tested.setSearchRequestFields(querySettings, srbMock);
			Mockito.verify(tested.configService).get(ConfigService.CFGNAME_SEARCH_RESPONSE_FIELDS);
			Mockito.verify(srbMock).addFields("_source");
			Mockito.verify(srbMock).setFetchSource(Mockito.any(String[].class), Mockito.any(String[].class));
			Mockito.verifyNoMoreInteractions(srbMock);
			Mockito.verifyNoMoreInteractions(tested.configService);
		}

		// case - filtering configured, role1 and role2 users have filtered parts of source
		{
			Mockito.reset(srbMock, tested.configService, tested.authenticationUtilService);
			mockAuthenticatedUserWithRole(tested, "role1");
			Mockito.when(tested.configService.get(ConfigService.CFGNAME_SEARCH_RESPONSE_FIELDS)).thenReturn(mockConfig);
			Mockito.when(srbMock.setFetchSource(Mockito.any(String[].class), Mockito.any(String[].class))).thenAnswer(
					new SourceExcludeMatcher(srbMock, TestUtils.createListOfStrings("dd")));

			tested.setSearchRequestFields(querySettings, srbMock);
			Mockito.verify(tested.configService).get(ConfigService.CFGNAME_SEARCH_RESPONSE_FIELDS);
			Mockito.verify(srbMock).addFields("_source");
			Mockito.verify(srbMock).setFetchSource(Mockito.any(String[].class), Mockito.any(String[].class));
			Mockito.verifyNoMoreInteractions(srbMock);
			Mockito.verifyNoMoreInteractions(tested.configService);
		}
		{
			Mockito.reset(srbMock, tested.configService, tested.authenticationUtilService);
			mockAuthenticatedUserWithRole(tested, "role2");
			Mockito.when(tested.configService.get(ConfigService.CFGNAME_SEARCH_RESPONSE_FIELDS)).thenReturn(mockConfig);
			Mockito.when(srbMock.setFetchSource(Mockito.any(String[].class), Mockito.any(String[].class))).thenAnswer(
					new SourceExcludeMatcher(srbMock, TestUtils.createListOfStrings("bb", "*.aa")));

			tested.setSearchRequestFields(querySettings, srbMock);
			Mockito.verify(tested.configService).get(ConfigService.CFGNAME_SEARCH_RESPONSE_FIELDS);
			Mockito.verify(srbMock).addFields("_source");
			Mockito.verify(srbMock).setFetchSource(Mockito.any(String[].class), Mockito.any(String[].class));
			Mockito.verifyNoMoreInteractions(srbMock);
			Mockito.verifyNoMoreInteractions(tested.configService);
		}

		// case - filtering configured, admin can see all fields, no any source filtering applied
		{
			Mockito.reset(srbMock, tested.configService, tested.authenticationUtilService);
			mockAuthenticatedUserWithRole(tested, Role.ADMIN);
			Mockito.when(tested.configService.get(ConfigService.CFGNAME_SEARCH_RESPONSE_FIELDS)).thenReturn(mockConfig);

			tested.setSearchRequestFields(querySettings, srbMock);
			Mockito.verify(tested.configService).get(ConfigService.CFGNAME_SEARCH_RESPONSE_FIELDS);
			Mockito.verify(srbMock).addFields("_source");
			Mockito.verifyNoMoreInteractions(srbMock);
			Mockito.verifyNoMoreInteractions(tested.configService);
		}
	}

	private static final class SourceExcludeMatcher implements Answer<SearchRequestBuilder> {

		private SearchRequestBuilder srbMock;
		private ArrayList<String> expectedExcludedFields;

		public SourceExcludeMatcher(SearchRequestBuilder srbMockToReturn, ArrayList<String> expectedExcludedFields) {
			this.srbMock = srbMockToReturn;
			this.expectedExcludedFields = expectedExcludedFields;
		}

		@Override
		public SearchRequestBuilder answer(InvocationOnMock invocation) throws Throwable {

			Assert.assertNull(invocation.getArguments()[0]);

			String[] actualStrings = (String[]) invocation.getArguments()[1];

			Assert.assertEquals("size of expected and actual list of strings is not same", expectedExcludedFields.size(),
					actualStrings.length);
			for (String s : actualStrings) {
				Assert.assertTrue(s + " is not in expected strings", expectedExcludedFields.contains(s));
			}

			return srbMock;
		}

	}

	@Test
	public void handleSortingSettings() {
		SearchService tested = getTested(Mockito.mock(ConfigService.class));
		SearchRequestBuilder srbMock = Mockito.mock(SearchRequestBuilder.class);

		// case - NPE when no settings passed in
		try {
			tested.setSearchRequestSort(null, srbMock);
			Assert.fail("NullPointerException expected");
		} catch (NullPointerException e) {
			Mockito.verifyNoMoreInteractions(srbMock);
			// OK
		}

		// case - sorting not requested
		{
			Mockito.reset(srbMock);
			QuerySettings querySettings = new QuerySettings();
			tested.setSearchRequestSort(querySettings, srbMock);
			Mockito.verifyNoMoreInteractions(srbMock);
		}

		// case - sorting requests
		{
			Mockito.reset(srbMock);
			QuerySettings querySettings = new QuerySettings();
			querySettings.setSortBy(SortByValue.SCORE);
			tested.setSearchRequestSort(querySettings, srbMock);
			Mockito.verifyNoMoreInteractions(srbMock);
		}
		{
			Mockito.reset(srbMock);
			QuerySettings querySettings = new QuerySettings();
			querySettings.setSortBy(SortByValue.NEW);
			tested.setSearchRequestSort(querySettings, srbMock);
			Mockito.verify(srbMock).addSort("sys_last_activity_date", SortOrder.DESC);
			Mockito.verifyNoMoreInteractions(srbMock);
		}
		{
			Mockito.reset(srbMock);
			QuerySettings querySettings = new QuerySettings();
			querySettings.setSortBy(SortByValue.OLD);
			tested.setSearchRequestSort(querySettings, srbMock);
			Mockito.verify(srbMock).addSort("sys_last_activity_date", SortOrder.ASC);
			Mockito.verifyNoMoreInteractions(srbMock);
		}
		{
			Mockito.reset(srbMock);
			QuerySettings querySettings = new QuerySettings();
			querySettings.setSortBy(SortByValue.NEW_CREATION);
			tested.setSearchRequestSort(querySettings, srbMock);
			Mockito.verify(srbMock).addSort("sys_created", SortOrder.DESC);
			Mockito.verifyNoMoreInteractions(srbMock);
		}
	}

	@Test
	public void getFilters() {
		{
			FilterBuilder[] ret = SearchService.filtersMapToArrayExcluding(null, null);
			Assert.assertNotNull(ret);
			Assert.assertEquals(0, ret.length);
		}
		{
			FilterBuilder[] ret = SearchService.filtersMapToArrayExcluding(null, "aaa");
			Assert.assertNotNull(ret);
			Assert.assertEquals(0, ret.length);
		}

		Map<String, FilterBuilder> filters = new LinkedHashMap<>();
		FilterBuilder fb1 = Mockito.mock(FilterBuilder.class);
		filters.put("f1", fb1);
		FilterBuilder fb2 = Mockito.mock(FilterBuilder.class);
		filters.put("f2", fb2);
		FilterBuilder fb3 = Mockito.mock(FilterBuilder.class);
		filters.put("f3", fb3);
		{
			FilterBuilder[] ret = SearchService.filtersMapToArrayExcluding(filters, null);
			Assert.assertNotNull(ret);
			Assert.assertEquals(3, ret.length);
			Assert.assertEquals(fb1, ret[0]);
			Assert.assertEquals(fb2, ret[1]);
			Assert.assertEquals(fb3, ret[2]);
		}

		{
			FilterBuilder[] ret = SearchService.filtersMapToArrayExcluding(filters, "f2");
			Assert.assertNotNull(ret);
			Assert.assertEquals(2, ret.length);
			Assert.assertEquals(fb1, ret[0]);
			Assert.assertEquals(fb3, ret[1]);
		}
	}

	@Test
	public void handleAggregationSettings_common() throws IOException, JSONException, ReflectiveOperationException {

		Client client = Mockito.mock(Client.class);
		ConfigService configService = mockConfigurationService();

		SearchService tested = getTested(configService);

		// case - no aggregations requested
		{
			SearchRequestBuilder srbMock = new SearchRequestBuilder(client);
			QuerySettings querySettings = new QuerySettings();
			tested.handleAggregationSettings(querySettings, null, srbMock);
			Assert.assertEquals("{ }", srbMock.toString());
		}

		// case - one aggregation requested without filters
		{
			SearchRequestBuilder srbMock = new SearchRequestBuilder(client);
			QuerySettings querySettings = new QuerySettings();
			querySettings.addAggregation("per_sys_type_counts");

			tested.handleAggregationSettings(querySettings, null, srbMock);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_aggregations_per_sys_type_counts.json", srbMock.toString());
		}

		// case - more aggregations requested without filters
		{
			SearchRequestBuilder srbMock = new SearchRequestBuilder(client);
			QuerySettings querySettings = new QuerySettings();
			querySettings.addAggregation("activity_dates_histogram");
			querySettings.addAggregation("per_project_counts");
			querySettings.addAggregation("tag_cloud");
			querySettings.addAggregation("top_contributors");
			tested.handleAggregationSettings(querySettings, null, srbMock);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_aggregations_moreNoFilter.json", srbMock.toString());
		}

		// case - more aggregations requested with more filters
		{
			SearchRequestBuilder srbMock = new SearchRequestBuilder(client);
			QuerySettings querySettings = new QuerySettings();
			Filters filters = new Filters();
			filters.acknowledgeUrlFilterCandidate("activity_date_from",
					new DateTime(100000l).toString(DATE_TIME_FORMATTER_UTC));
			filters
					.acknowledgeUrlFilterCandidate("activity_date_to", new DateTime(100200l).toString(DATE_TIME_FORMATTER_UTC));
			filters.acknowledgeUrlFilterCandidate("type", "my_content_type");
			filters.acknowledgeUrlFilterCandidate("contributor", "my_contributor_1", "my_contributor_2");
			filters.acknowledgeUrlFilterCandidate("content_provider", "my_sys_content_provider");
			filters.acknowledgeUrlFilterCandidate("sys_type", "my_sys_type", "my_sys_type_2");
			filters.acknowledgeUrlFilterCandidate("project", "my_project", "my_project_2");
			filters.acknowledgeUrlFilterCandidate("tag", "tag1", "tag2");
			tested.parsedFilterConfigService.prepareFiltersForRequest(filters);
			querySettings.setFilters(filters);
			querySettings.addAggregation("per_sys_type_counts");
			querySettings.addAggregation("activity_dates_histogram");
			querySettings.addAggregation("per_project_counts");
			querySettings.addAggregation("tag_cloud");
			querySettings.addAggregation("top_contributors");
			tested.handleAggregationSettings(querySettings, tested.parsedFilterConfigService.getSearchFiltersForRequest(), srbMock);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_aggregations_moreWithFilter.json", srbMock.toString());
		}
	}

	@Test
	public void handleAggregationSettings_top_contributors() throws IOException, JSONException, ReflectiveOperationException {

		Client client = Mockito.mock(Client.class);
		ConfigService configService = mockConfigurationService();

		SearchService tested = getTested(configService);

		// case - no contributor filter used, so only one top_contributors aggregation
		{
			SearchRequestBuilder srbMock = new SearchRequestBuilder(client);
			QuerySettings querySettings = new QuerySettings();
			querySettings.addAggregation("top_contributors");
			Filters filters = new Filters();
			filters.acknowledgeUrlFilterCandidate("tag", "tag1", "tag2");
			tested.parsedFilterConfigService.prepareFiltersForRequest(filters);
			querySettings.setFilters(filters);
			tested.handleAggregationSettings(querySettings, tested.parsedFilterConfigService.getSearchFiltersForRequest(), srbMock);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_aggregations_top_contributors_1.json", srbMock.toString());
		}

		// case - contributor filter used, so two top_contributors aggregations used
		{
			SearchRequestBuilder srbMock = new SearchRequestBuilder(client);
			QuerySettings querySettings = new QuerySettings();
			querySettings.addAggregation("top_contributors");
			Filters filters = new Filters();
			filters.acknowledgeUrlFilterCandidate("tag", "tag1", "tag2");
			filters.acknowledgeUrlFilterCandidate("contributor", "John Doe <john@doe.org>");
			tested.parsedFilterConfigService.prepareFiltersForRequest(filters);
			querySettings.setFilters(filters);
			tested.handleAggregationSettings(querySettings, tested.parsedFilterConfigService.getSearchFiltersForRequest(), srbMock);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_aggregations_top_contributors_2.json", srbMock.toString());
		}
	}

	@Test
	public void handleAggregationSettings_activity_dates_histogram() throws IOException, JSONException,
			ReflectiveOperationException {

		Client client = Mockito.mock(Client.class);
		ConfigService configService = mockConfigurationService();

		SearchService tested = getTested(configService);

		// case - no activity dates filter used
		{
			SearchRequestBuilder srbMock = new SearchRequestBuilder(client);
			QuerySettings querySettings = new QuerySettings();
			querySettings.addAggregation("activity_dates_histogram");
			Filters filters = new Filters();
			filters.acknowledgeUrlFilterCandidate("tag", "tag1", "tag2");
			tested.parsedFilterConfigService.prepareFiltersForRequest(filters);
			querySettings.setFilters(filters);
			tested.handleAggregationSettings(querySettings, tested.parsedFilterConfigService.getSearchFiltersForRequest(), srbMock);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_aggregations_activity_dates_histogram_1.json",
					srbMock.toString());
		}

		// case - activity dates interval filter used (so from/to is ignored)
		{
			SearchRequestBuilder srbMock = new SearchRequestBuilder(client);
			QuerySettings querySettings = new QuerySettings();
			querySettings.addAggregation("activity_dates_histogram");
			Filters filters = new Filters();
			filters.acknowledgeUrlFilterCandidate("tag", "tag1", "tag2");
			filters.acknowledgeUrlFilterCandidate("activity_date_interval", PastIntervalValue.TEST.toString());
			filters.acknowledgeUrlFilterCandidate("activity_date_from",
					new DateTime(1256545l).toString(DATE_TIME_FORMATTER_UTC));
			filters.acknowledgeUrlFilterCandidate("activity_date_to",
					new DateTime(2256545l).toString(DATE_TIME_FORMATTER_UTC));
			tested.parsedFilterConfigService.prepareFiltersForRequest(filters);
			querySettings.setFilters(filters);
			tested.handleAggregationSettings(querySettings, tested.parsedFilterConfigService.getSearchFiltersForRequest(), srbMock);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_aggregations_activity_dates_histogram_2.json",
					srbMock.toString());
		}

		// case - activity dates from/to filter used
		{
			SearchRequestBuilder srbMock = new SearchRequestBuilder(client);
			QuerySettings querySettings = new QuerySettings();
			querySettings.addAggregation("activity_dates_histogram");
			Filters filters = new Filters();
			filters.acknowledgeUrlFilterCandidate("tag", "tag1", "tag2");
			filters.acknowledgeUrlFilterCandidate("activity_date_from",
					new DateTime(1256545l).toString(DATE_TIME_FORMATTER_UTC));
			filters.acknowledgeUrlFilterCandidate("activity_date_to",
					new DateTime(22256545l).toString(DATE_TIME_FORMATTER_UTC));
			tested.parsedFilterConfigService.prepareFiltersForRequest(filters);
			querySettings.setFilters(filters);
			tested.handleAggregationSettings(querySettings, tested.parsedFilterConfigService.getSearchFiltersForRequest(), srbMock);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_aggregations_activity_dates_histogram_3.json",
					srbMock.toString());
		}
	}

	/**
	 * This test demonstrate how to test complete Elasticsearch query (and not only individual parts of it).
	 * 
	 * @see <a href="https://github.com/searchisko/searchisko/issues/130">This test was inspired by #130.</a>
	 * 
	 * @throws IOException
	 * @throws JSONException
	 * @throws ReflectiveOperationException
	 */
	@Test
	public void testCompleteElasticsearchQuery() throws IOException, JSONException, ReflectiveOperationException {

		Client client = Mockito.mock(Client.class);
		ConfigService configService = mockConfigurationService();

		SearchService tested = getTested(configService);

		QuerySettings querySettings = new QuerySettings();
		Filters filters = new Filters();
		querySettings.setFilters(filters);

		tested.parsedFilterConfigService.prepareFiltersForRequest(filters);

		mockProviderConfiguration(tested, "/search/provider_1.json", "/search/provider_2.json");

		{
			SearchRequestBuilder srb = new SearchRequestBuilder(client);
			querySettings.addAggregation("activity_dates_histogram");
			querySettings.addAggregation("per_project_counts");
			querySettings.addAggregation("per_sys_type_counts");
			querySettings.setQuery("This is client query");
			filters.acknowledgeUrlFilterCandidate("activity_date_interval", PastIntervalValue.TEST.toString());
			filters.acknowledgeUrlFilterCandidate("project", "eap");
			filters.acknowledgeUrlFilterCandidate("sys_type", "issue");
			querySettings.setSize(0);
			tested.parsedFilterConfigService.prepareFiltersForRequest(filters);
			querySettings.setFilters(filters);
			tested.performSearchInternal(querySettings, srb);
			TestUtils.assertJsonContentFromClasspathFile("/search/complete_query_filters_and_activity_dates_histogram.json",
					srb.toString());
			// We should assert indices and types in srb there, but it is not possible due ES API lack of getters !!!
		}
	}

	@Test
	public void applyContentLevelSecurityFilter() throws IOException, JSONException {

		ConfigService configService = mockConfigurationService();
		SearchService tested = getTested(configService);

		// case - not logged in user
		QueryBuilder qb = QueryBuilders.matchAllQuery();

		{
			Mockito.when(tested.authenticationUtilService.isAuthenticatedUser()).thenReturn(false);
			QueryBuilder retqb = tested.applyContentLevelSecurityFilter(qb);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_dlsecurity_filter_anonym.json", retqb.toString());
		}

		// case - admin user
		{
			Mockito.reset(tested.authenticationUtilService);
			Mockito.when(tested.authenticationUtilService.isUserInRole(Role.ADMIN)).thenReturn(true);
			Mockito.when(tested.authenticationUtilService.isAuthenticatedUser()).thenReturn(true);

			QueryBuilder retqb = tested.applyContentLevelSecurityFilter(qb);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_dlsecurity_filter_admin.json", retqb.toString());

			Mockito.verify(tested.authenticationUtilService).isUserInRole(Role.ADMIN);
			Mockito.verifyNoMoreInteractions(tested.authenticationUtilService);
		}

		// case - authenticated user with roles
		{
			Mockito.reset(tested.authenticationUtilService);
			Mockito.when(tested.authenticationUtilService.isUserInRole(Role.ADMIN)).thenReturn(false);
			Mockito.when(tested.authenticationUtilService.getUserRoles()).thenReturn(
					TestUtils.createSetOfStrings("role1", "role2"));
			Mockito.when(tested.authenticationUtilService.isAuthenticatedUser()).thenReturn(true);

			QueryBuilder retqb = tested.applyContentLevelSecurityFilter(qb);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_dlsecurity_filter_userwithroles.json",
					retqb.toString());

			Mockito.verify(tested.authenticationUtilService).isUserInRole(Role.ADMIN);
			Mockito.verify(tested.authenticationUtilService).isAuthenticatedUser();
			Mockito.verify(tested.authenticationUtilService).getUserRoles();
			Mockito.verifyNoMoreInteractions(tested.authenticationUtilService);
		}

	}

	@Test
	public void selectActivityDatesHistogramInterval_common() throws ReflectiveOperationException {

		ConfigService configService = mockConfigurationService();

		SearchService tested = getTested(configService);

		tested.parsedFilterConfigService.prepareFiltersForRequest(null);
		String fieldName = "sys_activity_dates";

		// case - no activity dates filter defined
		Assert.assertEquals("month", tested.getDateHistogramAggregationInterval(null));

		Assert.assertEquals("month", tested.getDateHistogramAggregationInterval(fieldName));

		Filters filters = new Filters();
		// case - activity date interval precedence against from/to
		filters.acknowledgeUrlFilterCandidate("activity_date_interval", PastIntervalValue.YEAR.toString());
		filters.acknowledgeUrlFilterCandidate("activity_date_from",
				new DateTime(DateTimeZone.UTC).minus(1000000).toString(DATE_TIME_FORMATTER_UTC));
		filters.acknowledgeUrlFilterCandidate("activity_date_to", new DateTime(DateTimeZone.UTC).toString(DATE_TIME_FORMATTER_UTC));
		tested.parsedFilterConfigService.prepareFiltersForRequest(filters);
		Assert.assertEquals("week", tested.getDateHistogramAggregationInterval(fieldName));
	}

	@Test
	public void selectActivityDatesHistogramInterval_intervalFilter() throws ReflectiveOperationException {

		ConfigService configService = mockConfigurationService();
		SearchService tested = getTested(configService);

		String fieldName = "sys_activity_dates";
		Filters filters = new Filters();

		filters.acknowledgeUrlFilterCandidate("activity_date_interval", PastIntervalValue.YEAR.toString());
		tested.parsedFilterConfigService.prepareFiltersForRequest(filters);
		Assert.assertEquals("week", tested.getDateHistogramAggregationInterval(fieldName));

		filters.acknowledgeUrlFilterCandidate("activity_date_interval", PastIntervalValue.QUARTER.toString());
		tested.parsedFilterConfigService.prepareFiltersForRequest(filters);
		Assert.assertEquals("week", tested.getDateHistogramAggregationInterval(fieldName));

		filters.acknowledgeUrlFilterCandidate("activity_date_interval", PastIntervalValue.MONTH.toString());
		tested.parsedFilterConfigService.prepareFiltersForRequest(filters);
		Assert.assertEquals("day", tested.getDateHistogramAggregationInterval(fieldName));

		filters.acknowledgeUrlFilterCandidate("activity_date_interval", PastIntervalValue.WEEK.toString());
		tested.parsedFilterConfigService.prepareFiltersForRequest(filters);
		Assert.assertEquals("day", tested.getDateHistogramAggregationInterval(fieldName));

		filters.acknowledgeUrlFilterCandidate("activity_date_interval", PastIntervalValue.DAY.toString());
		tested.parsedFilterConfigService.prepareFiltersForRequest(filters);
		Assert.assertEquals("hour", tested.getDateHistogramAggregationInterval(fieldName));
	}

	@Test
	public void selectActivityDatesHistogramInterval_fromToFilter() throws ReflectiveOperationException {

		ConfigService configService = mockConfigurationService();
		SearchService tested = getTested(configService);

		String fieldName = "sys_activity_dates";
		Filters filters = new Filters();

		// case - no from defined, so always month
		filters.forgetUrlFilterCandidate("activity_date_from", "activity_date_to");
		filters.acknowledgeUrlFilterCandidate("activity_date_from");
		filters.acknowledgeUrlFilterCandidate("activity_date_to", new DateTime(DateTimeZone.UTC).toString(DATE_TIME_FORMATTER_UTC));
		tested.parsedFilterConfigService.prepareFiltersForRequest(filters);
		Assert.assertEquals("month", tested.getDateHistogramAggregationInterval(fieldName));

		filters.forgetUrlFilterCandidate("activity_date_from", "activity_date_to");
		filters.acknowledgeUrlFilterCandidate("activity_date_to",
				new DateTime(DateTimeZone.UTC).minusYears(10).toString(DATE_TIME_FORMATTER_UTC));
		tested.parsedFilterConfigService.prepareFiltersForRequest(filters);
		Assert.assertEquals("month", tested.getDateHistogramAggregationInterval(fieldName));

		// case - no to defined means current timestamp is used
		filters.forgetUrlFilterCandidate("activity_date_from", "activity_date_to");
		filters.acknowledgeUrlFilterCandidate("activity_date_from", new DateTime(DateTimeZone.UTC).toString(DATE_TIME_FORMATTER_UTC));
		filters.acknowledgeUrlFilterCandidate("activity_date_to");
		tested.parsedFilterConfigService.prepareFiltersForRequest(filters);
		Assert.assertEquals("minute", tested.getDateHistogramAggregationInterval(fieldName));

		// clear cache
		filters.forgetUrlFilterCandidate("activity_date_from", "activity_date_to");
		filters.acknowledgeUrlFilterCandidate("activity_date_from",
				new DateTime(DateTimeZone.UTC).minusHours(1).plusMillis(100).toString(DATE_TIME_FORMATTER_UTC));
		tested.parsedFilterConfigService.prepareFiltersForRequest(filters);
		Assert.assertEquals("minute", tested.getDateHistogramAggregationInterval(fieldName));
		filters.acknowledgeUrlFilterCandidate("activity_date_from",
				new DateTime(DateTimeZone.UTC).minusHours(1).minusMillis(100).toString(DATE_TIME_FORMATTER_UTC));
		tested.parsedFilterConfigService.prepareFiltersForRequest(filters);
		Assert.assertEquals("hour", tested.getDateHistogramAggregationInterval(fieldName));

		filters.forgetUrlFilterCandidate("activity_date_from", "activity_date_to");
		filters.acknowledgeUrlFilterCandidate("activity_date_from",
				new DateTime(DateTimeZone.UTC).minusDays(2).plusMillis(100).toString(DATE_TIME_FORMATTER_UTC));
		tested.parsedFilterConfigService.prepareFiltersForRequest(filters);
		Assert.assertEquals("hour", tested.getDateHistogramAggregationInterval(fieldName));
		filters.acknowledgeUrlFilterCandidate("activity_date_from",
				new DateTime(DateTimeZone.UTC).minusDays(2).minusMillis(100).toString(DATE_TIME_FORMATTER_UTC));
		tested.parsedFilterConfigService.prepareFiltersForRequest(filters);
		Assert.assertEquals("day", tested.getDateHistogramAggregationInterval(fieldName));

		filters.forgetUrlFilterCandidate("activity_date_from", "activity_date_to");
		filters.acknowledgeUrlFilterCandidate("activity_date_from", new DateTime(System.currentTimeMillis() - 1000L * 60L
				* 60L * 24l * 7l * 8l + 100l).toString(DATE_TIME_FORMATTER_UTC));
		tested.parsedFilterConfigService.prepareFiltersForRequest(filters);
		Assert.assertEquals("day", tested.getDateHistogramAggregationInterval(fieldName));
		filters.acknowledgeUrlFilterCandidate("activity_date_from", new DateTime(System.currentTimeMillis() - 1000L * 60L
				* 60L * 24l * 7l * 8l - 100l).toString(DATE_TIME_FORMATTER_UTC));
		tested.parsedFilterConfigService.prepareFiltersForRequest(filters);
		Assert.assertEquals("week", tested.getDateHistogramAggregationInterval(fieldName));

		filters.forgetUrlFilterCandidate("activity_date_from", "activity_date_to");
		filters.acknowledgeUrlFilterCandidate("activity_date_from",
				new DateTime(DateTimeZone.UTC).minusDays(366).plusMillis(100).toString(DATE_TIME_FORMATTER_UTC));
		tested.parsedFilterConfigService.prepareFiltersForRequest(filters);
		Assert.assertEquals("week", tested.getDateHistogramAggregationInterval(fieldName));
		filters.acknowledgeUrlFilterCandidate("activity_date_from", new DateTime(DateTimeZone.UTC).minusDays(366).minusMillis(100)
				.toString(DATE_TIME_FORMATTER_UTC));
		tested.parsedFilterConfigService.prepareFiltersForRequest(filters);
		Assert.assertEquals("month", tested.getDateHistogramAggregationInterval(fieldName));

		// case - both from and to defined
		filters.forgetUrlFilterCandidate("activity_date_from", "activity_date_to");
		filters.acknowledgeUrlFilterCandidate("activity_date_from", new DateTime(1000L).toString(DATE_TIME_FORMATTER_UTC));
		filters.acknowledgeUrlFilterCandidate("activity_date_to", new DateTime(1200L).toString(DATE_TIME_FORMATTER_UTC));
		tested.parsedFilterConfigService.prepareFiltersForRequest(filters);
		Assert.assertEquals("minute", tested.getDateHistogramAggregationInterval(fieldName));

		filters.acknowledgeUrlFilterCandidate("activity_date_to",
				new DateTime(1000L + 1000L * 60L * 60L - 100L).toString(DATE_TIME_FORMATTER_UTC));
		tested.parsedFilterConfigService.prepareFiltersForRequest(filters);
		Assert.assertEquals("minute", tested.getDateHistogramAggregationInterval(fieldName));
		filters.acknowledgeUrlFilterCandidate("activity_date_to",
				new DateTime(1000L + 1000L * 60L * 60L + 100L).toString(DATE_TIME_FORMATTER_UTC));
		tested.parsedFilterConfigService.prepareFiltersForRequest(filters);
		Assert.assertEquals("hour", tested.getDateHistogramAggregationInterval(fieldName));

		filters.acknowledgeUrlFilterCandidate("activity_date_from",
				new DateTime(1000000L).toString(DATE_TIME_FORMATTER_UTC));
		filters.acknowledgeUrlFilterCandidate("activity_date_to", new DateTime(1000000L + 1000L * 60L * 60L * 24l * 2l
				- 100L).toString(DATE_TIME_FORMATTER_UTC));
		tested.parsedFilterConfigService.prepareFiltersForRequest(filters);
		Assert.assertEquals("hour", tested.getDateHistogramAggregationInterval(fieldName));
		filters.acknowledgeUrlFilterCandidate("activity_date_to", new DateTime(1000000L + 1000L * 60L * 60L * 24l * 2l
				+ 100L).toString(DATE_TIME_FORMATTER_UTC));
		tested.parsedFilterConfigService.prepareFiltersForRequest(filters);
		Assert.assertEquals("day", tested.getDateHistogramAggregationInterval(fieldName));

		filters.acknowledgeUrlFilterCandidate("activity_date_from",
				new DateTime(100000000L).toString(DATE_TIME_FORMATTER_UTC));
		filters.acknowledgeUrlFilterCandidate("activity_date_to", new DateTime(100000000L + 1000L * 60L * 60L * 24l * 7l
				* 8l - 100L).toString(DATE_TIME_FORMATTER_UTC));
		tested.parsedFilterConfigService.prepareFiltersForRequest(filters);
		Assert.assertEquals("day", tested.getDateHistogramAggregationInterval(fieldName));
		filters.acknowledgeUrlFilterCandidate("activity_date_to", new DateTime(100000000L + 1000L * 60L * 60L * 24l * 7l
				* 8l + 100L).toString(DATE_TIME_FORMATTER_UTC));
		tested.parsedFilterConfigService.prepareFiltersForRequest(filters);
		Assert.assertEquals("week", tested.getDateHistogramAggregationInterval(fieldName));

		filters.acknowledgeUrlFilterCandidate("activity_date_from",
				new DateTime(1000000000L).toString(DATE_TIME_FORMATTER_UTC));
		filters.acknowledgeUrlFilterCandidate("activity_date_to", new DateTime(1000000000L + 1000L * 60L * 60L * 24L * 366L
				- 100L).toString(DATE_TIME_FORMATTER_UTC));
		tested.parsedFilterConfigService.prepareFiltersForRequest(filters);
		Assert.assertEquals("week", tested.getDateHistogramAggregationInterval(fieldName));
		filters.acknowledgeUrlFilterCandidate("activity_date_to", new DateTime(1000000000L + 1000L * 60L * 60L * 24L * 366L
				+ 100L).toString(DATE_TIME_FORMATTER_UTC));
		tested.parsedFilterConfigService.prepareFiltersForRequest(filters);
		Assert.assertEquals("month", tested.getDateHistogramAggregationInterval(fieldName));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void writeSearchHitUsedStatisticsRecord() {
		SearchService tested = getTested(null);

		// case - record not accepted
		{
			Mockito.reset(tested.statsClientService);
			Mockito.when(
					tested.statsClientService.checkStatisticsRecordExists(Mockito.eq(StatsRecordType.SEARCH), Mockito.anyMap()))
					.thenAnswer(new Answer<Boolean>() {

						@Override
						public Boolean answer(InvocationOnMock invocation) throws Throwable {
							Map<String, String> conditions = (Map<String, String>) invocation.getArguments()[1];
							Assert.assertEquals(2, conditions.size());
							Assert.assertEquals("my-uuid", conditions.get(StatsClientService.FIELD_RESPONSE_UUID));
							Assert.assertEquals("my_hit_id", conditions.get(StatsClientService.FIELD_HITS_ID));
							return false;
						}

					});
			Assert.assertFalse(tested.writeSearchHitUsedStatisticsRecord("my-uuid", "my_hit_id", null));
			Mockito.verify(tested.statsClientService).checkStatisticsRecordExists(Mockito.eq(StatsRecordType.SEARCH),
					Mockito.anyMap());
			Mockito.verifyNoMoreInteractions(tested.statsClientService);
		}

		// case - record accepted
		{
			Mockito.reset(tested.statsClientService);
			Mockito.when(
					tested.statsClientService.checkStatisticsRecordExists(Mockito.eq(StatsRecordType.SEARCH), Mockito.anyMap()))
					.thenAnswer(new Answer<Boolean>() {

						@Override
						public Boolean answer(InvocationOnMock invocation) throws Throwable {
							Map<String, String> conditions = (Map<String, String>) invocation.getArguments()[1];
							Assert.assertEquals(2, conditions.size());
							Assert.assertEquals("my-uuid", conditions.get(StatsClientService.FIELD_RESPONSE_UUID));
							Assert.assertEquals("my_hit_id", conditions.get(StatsClientService.FIELD_HITS_ID));
							return true;
						}

					});
			Mockito.doAnswer(new Answer() {

				@Override
				public Object answer(InvocationOnMock invocation) throws Throwable {
					Map<String, String> conditions = (Map<String, String>) invocation.getArguments()[2];
					Assert.assertEquals(3, conditions.size());
					Assert.assertEquals("my-uuid", conditions.get(StatsClientService.FIELD_RESPONSE_UUID));
					Assert.assertEquals("my_hit_id", conditions.get(StatsClientService.FIELD_HITS_ID));
					Assert.assertEquals("my-session-id", conditions.get("session"));
					Long ts = (Long) invocation.getArguments()[1];
					long cts = System.currentTimeMillis();
					Assert.assertTrue("passed in timestamp is invalid, must be nearly current", (cts - 2000) <= ts && ts <= cts);
					return null;
				}
			}).when(tested.statsClientService)
					.writeStatisticsRecord(Mockito.eq(StatsRecordType.SEARCH_HIT_USED), Mockito.anyLong(), Mockito.anyMap());

			Assert.assertTrue(tested.writeSearchHitUsedStatisticsRecord("my-uuid", "my_hit_id", "my-session-id"));

			Mockito.verify(tested.statsClientService).checkStatisticsRecordExists(Mockito.eq(StatsRecordType.SEARCH),
					Mockito.anyMap());
			Mockito.verify(tested.statsClientService).writeStatisticsRecord(Mockito.eq(StatsRecordType.SEARCH_HIT_USED),
					Mockito.anyLong(), Mockito.anyMap());
			Mockito.verifyNoMoreInteractions(tested.statsClientService);
		}
	}

}
