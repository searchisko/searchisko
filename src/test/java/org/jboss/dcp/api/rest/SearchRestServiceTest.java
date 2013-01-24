/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.core.UriInfo;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.jboss.dcp.api.model.QuerySettings;
import org.jboss.dcp.api.model.QuerySettings.Filters;
import org.jboss.dcp.api.model.QuerySettings.SortByValue;
import org.jboss.dcp.api.service.ConfigService;
import org.jboss.dcp.api.service.ProviderService;
import org.jboss.dcp.api.testtools.TestUtils;
import org.jboss.dcp.api.util.QuerySettingsParser.PastIntervalName;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

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

	@SuppressWarnings("unchecked")
	@Test
	public void handleSearchInicesAndTypes() {
		SearchRestService tested = new SearchRestService();
		tested.providerService = Mockito.mock(ProviderService.class);
		tested.log = Logger.getLogger("testlogger");

		QuerySettings querySettings = new QuerySettings();
		SearchRequestBuilder searchRequestBuilderMock = Mockito.mock(SearchRequestBuilder.class);

		// case - searching for all types, no provider defined
		{
			List<Map<String, Object>> mockedProvidersList = new ArrayList<Map<String, Object>>();
			Mockito.when(tested.providerService.listAllProviders()).thenReturn(mockedProvidersList);
			tested.handleSearchInicesAndTypes(querySettings, searchRequestBuilderMock);
			Mockito.verify(tested.providerService).listAllProviders();
			Mockito.verify(searchRequestBuilderMock).setIndices(new String[] {});
			Mockito.verifyNoMoreInteractions(searchRequestBuilderMock);
		}

		Filters filters = new Filters();
		querySettings.setFilters(filters);

		// case - searching for all types, some providers defined with all possible combinations of index definitions
		{
			Mockito.reset(tested.providerService, searchRequestBuilderMock);
			List<Map<String, Object>> mockedProvidersList = new ArrayList<Map<String, Object>>();
			mockedProvidersList.add(TestUtils.loadJSONFromClasspathFile("/search/provider_1.json"));
			mockedProvidersList.add(TestUtils.loadJSONFromClasspathFile("/search/provider_2.json"));
			Mockito.when(tested.providerService.listAllProviders()).thenReturn(mockedProvidersList);
			tested.handleSearchInicesAndTypes(querySettings, searchRequestBuilderMock);
			Mockito.verify(tested.providerService).listAllProviders();
			Mockito.verify(searchRequestBuilderMock).setIndices(
					new String[] { "idx_provider1_issue", "idx_provider1_mailing1", "idx_provider1_mailing2",
							"idx_provider2_mailing", "idx_provider2_issue1", "idx_provider2_issue2" });
			Mockito.verifyNoMoreInteractions(searchRequestBuilderMock);
		}

		// case - contentType filter used - type with one index
		{
			Mockito.reset(tested.providerService, searchRequestBuilderMock);
			String testedType = "provider1_issue";
			filters.setContentType(testedType);
			Mockito.when(tested.providerService.findContentType(testedType)).thenReturn(
					((Map<String, Map<String, Object>>) TestUtils.loadJSONFromClasspathFile("/search/provider_1.json").get(
							ProviderService.TYPE)).get(testedType));
			tested.handleSearchInicesAndTypes(querySettings, searchRequestBuilderMock);
			Mockito.verify(searchRequestBuilderMock).setIndices(new String[] { "idx_provider1_issue" });
			Mockito.verify(searchRequestBuilderMock).setTypes(new String[] { "t_provider1_issue" });
			Mockito.verifyNoMoreInteractions(searchRequestBuilderMock);
		}

		// case - contentType filter used - type with more search indices
		{
			Mockito.reset(tested.providerService, searchRequestBuilderMock);
			String testedType = "provider1_mailing";
			filters.setContentType(testedType);
			Mockito.when(tested.providerService.findContentType(testedType)).thenReturn(
					((Map<String, Map<String, Object>>) TestUtils.loadJSONFromClasspathFile("/search/provider_1.json").get(
							ProviderService.TYPE)).get(testedType));
			tested.handleSearchInicesAndTypes(querySettings, searchRequestBuilderMock);
			Mockito.verify(searchRequestBuilderMock).setIndices(
					new String[] { "idx_provider1_mailing1", "idx_provider1_mailing2" });
			Mockito.verify(searchRequestBuilderMock).setTypes(new String[] { "t_provider1_mailing" });
			Mockito.verifyNoMoreInteractions(searchRequestBuilderMock);
		}

		// case - contentType filter used - type with search_all_excluded=true can be used if named
		{
			Mockito.reset(tested.providerService, searchRequestBuilderMock);
			String testedType = "provider1_cosi";
			filters.setContentType(testedType);
			Mockito.when(tested.providerService.findContentType(testedType)).thenReturn(
					((Map<String, Map<String, Object>>) TestUtils.loadJSONFromClasspathFile("/search/provider_1.json").get(
							ProviderService.TYPE)).get(testedType));
			tested.handleSearchInicesAndTypes(querySettings, searchRequestBuilderMock);
			Mockito.verify(searchRequestBuilderMock)
					.setIndices(new String[] { "idx_provider1_cosi1", "idx_provider1_cosi2" });
			Mockito.verify(searchRequestBuilderMock).setTypes(new String[] { "t_provider1_cosi" });
			Mockito.verifyNoMoreInteractions(searchRequestBuilderMock);
		}

		filters = new Filters();
		querySettings.setFilters(filters);
		// case - dcp_type filter used
		{
			Mockito.reset(tested.providerService, searchRequestBuilderMock);
			filters.setDcpTypes(Arrays.asList(new String[] { "issue" }));
			List<Map<String, Object>> mockedProvidersList = new ArrayList<Map<String, Object>>();
			mockedProvidersList.add(TestUtils.loadJSONFromClasspathFile("/search/provider_1.json"));
			mockedProvidersList.add(TestUtils.loadJSONFromClasspathFile("/search/provider_2.json"));
			Mockito.when(tested.providerService.listAllProviders()).thenReturn(mockedProvidersList);
			tested.handleSearchInicesAndTypes(querySettings, searchRequestBuilderMock);
			Mockito.verify(tested.providerService).listAllProviders();
			Mockito.verify(searchRequestBuilderMock).setIndices(
					new String[] { "idx_provider1_issue", "idx_provider2_issue1", "idx_provider2_issue2" });
			Mockito.verifyNoMoreInteractions(searchRequestBuilderMock);
		}

		// case - dcp_type filter used - type with search_all_excluded=true can be used if named
		{
			Mockito.reset(tested.providerService, searchRequestBuilderMock);
			filters.setDcpTypes(Arrays.asList(new String[] { "cosi" }));
			List<Map<String, Object>> mockedProvidersList = new ArrayList<Map<String, Object>>();
			mockedProvidersList.add(TestUtils.loadJSONFromClasspathFile("/search/provider_1.json"));
			mockedProvidersList.add(TestUtils.loadJSONFromClasspathFile("/search/provider_2.json"));
			Mockito.when(tested.providerService.listAllProviders()).thenReturn(mockedProvidersList);
			tested.handleSearchInicesAndTypes(querySettings, searchRequestBuilderMock);
			Mockito.verify(tested.providerService).listAllProviders();
			Mockito.verify(searchRequestBuilderMock).setIndices(
					new String[] { "idx_provider1_cosi1", "idx_provider1_cosi2", "idx_provider2_cosi1", "idx_provider2_cosi2" });
			Mockito.verifyNoMoreInteractions(searchRequestBuilderMock);
		}

		// case - dcp_type filter used with multiple values
		{
			Mockito.reset(tested.providerService, searchRequestBuilderMock);
			filters.setDcpTypes(Arrays.asList("issue", "cosi"));
			List<Map<String, Object>> mockedProvidersList = new ArrayList<Map<String, Object>>();
			mockedProvidersList.add(TestUtils.loadJSONFromClasspathFile("/search/provider_1.json"));
			mockedProvidersList.add(TestUtils.loadJSONFromClasspathFile("/search/provider_2.json"));
			Mockito.when(tested.providerService.listAllProviders()).thenReturn(mockedProvidersList);
			tested.handleSearchInicesAndTypes(querySettings, searchRequestBuilderMock);
			Mockito.verify(tested.providerService).listAllProviders();
			Mockito.verify(searchRequestBuilderMock).setIndices(
					new String[] { "idx_provider1_cosi1", "idx_provider1_cosi2", "idx_provider1_issue", "idx_provider2_cosi1",
							"idx_provider2_cosi2", "idx_provider2_issue1", "idx_provider2_issue2" });
			Mockito.verifyNoMoreInteractions(searchRequestBuilderMock);
		}

	}

	@Test
	public void handleCommonFiltersSettings_moreFilters() throws IOException {
		SearchRestService tested = new SearchRestService();
		tested.log = Logger.getLogger("testlogger");

		// case - NPE when no settings passed in
		try {
			QueryBuilder qb = QueryBuilders.matchAllQuery();
			tested.handleCommonFiltersSettings(null, qb);
			Assert.fail("NullPointerException expected");
		} catch (NullPointerException e) {
			// OK
		}

		QuerySettings querySettings = new QuerySettings();

		// case - no filters object exists
		{
			QueryBuilder qb = QueryBuilders.matchAllQuery();
			QueryBuilder qbRes = tested.handleCommonFiltersSettings(querySettings, qb);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_match_all.json", qbRes.toString());
		}

		Filters filters = new Filters();
		querySettings.setFilters(filters);
		// case - empty filters object
		{
			QueryBuilder qb = QueryBuilders.matchAllQuery();
			QueryBuilder qbRes = tested.handleCommonFiltersSettings(querySettings, qb);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_match_all.json", qbRes.toString());
		}

		// case - all filters used
		{
			filters.setDcpTypes(Arrays.asList("myDcpType", "myDcpType2"));
			filters.setDcpContentProvider("my_content_provider");
			filters.setTags(Arrays.asList("tag1", "tag2"));
			filters.setProjects(Arrays.asList("pr1", "pr2"));
			filters.setContributors(Arrays.asList("John Doe <john@doe.com>", "Dan Boo <boo@boo.net>"));
			// activityDateInterval not tested here because variable, see separate test
			filters.setActivityDateFrom(1359232356456L);
			filters.setActivityDateTo(1359232366456L);
			QueryBuilder qb = QueryBuilders.matchAllQuery();
			QueryBuilder qbRes = tested.handleCommonFiltersSettings(querySettings, qb);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_filters_moreFilters.json", qbRes.toString());
		}
	}

	@Test
	public void handleCommonFiltersSettings_ActivityDateInterval() throws IOException {
		SearchRestService tested = new SearchRestService();
		tested.log = Logger.getLogger("testlogger");

		QuerySettings querySettings = new QuerySettings();
		Filters filters = new Filters();
		querySettings.setFilters(filters);
		filters.setActivityDateInterval(PastIntervalName.DAY);
		// set date_from and date_to to some values to test this is ignored if interval is used
		filters.setActivityDateTo(1359232366456L);
		filters.setActivityDateFrom(1359232356456L);
		{
			QueryBuilder qb = QueryBuilders.matchAllQuery();
			QueryBuilder qbRes = tested.handleCommonFiltersSettings(querySettings, qb);
			String s = qbRes.toString().replaceAll("[ \\n\\t]", "");
			Assert.assertTrue(s.contains("\"range\":{\"dcp_activity_dates\":{\"from\":\""));
			Assert.assertTrue(s.contains("\"to\":null"));
			Assert.assertFalse(s.contains("2013-01-26T20:32:46.456Z"));
			Assert.assertFalse(s.contains("2013-01-26T20:32:36.456Z"));
		}
	}

	@Test
	public void handleCommonFiltersSettings_ActivityDateFromTo() throws IOException {
		SearchRestService tested = new SearchRestService();
		tested.log = Logger.getLogger("testlogger");

		QuerySettings querySettings = new QuerySettings();
		Filters filters = new Filters();
		querySettings.setFilters(filters);

		// case - only from
		filters.setActivityDateFrom(1359232356456L);
		filters.setActivityDateTo(null);
		{
			QueryBuilder qb = QueryBuilders.matchAllQuery();
			QueryBuilder qbRes = tested.handleCommonFiltersSettings(querySettings, qb);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_filters_activityDateFrom.json", qbRes.toString());
		}

		// case - only to
		filters.setActivityDateFrom(null);
		filters.setActivityDateTo(1359232366456L);
		{
			QueryBuilder qb = QueryBuilders.matchAllQuery();
			QueryBuilder qbRes = tested.handleCommonFiltersSettings(querySettings, qb);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_filters_activityDateTo.json", qbRes.toString());
		}

		// case - both bounds
		filters.setActivityDateFrom(1359232356456L);
		filters.setActivityDateTo(1359232366456L);
		{
			QueryBuilder qb = QueryBuilders.matchAllQuery();
			QueryBuilder qbRes = tested.handleCommonFiltersSettings(querySettings, qb);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_filters_activityDateFromTo.json", qbRes.toString());
		}
	}

	@Test
	public void handleCommonFiltersSettings_projects() throws IOException {
		SearchRestService tested = new SearchRestService();
		tested.log = Logger.getLogger("testlogger");

		QuerySettings querySettings = new QuerySettings();
		Filters filters = new Filters();
		querySettings.setFilters(filters);
		// case - list of projects is null
		{
			QueryBuilder qb = QueryBuilders.matchAllQuery();
			QueryBuilder qbRes = tested.handleCommonFiltersSettings(querySettings, qb);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_match_all.json", qbRes.toString());
		}

		// case - list of projects is empty
		{
			filters.setProjects(Arrays.asList(new String[] {}));
			QueryBuilder qb = QueryBuilders.matchAllQuery();
			QueryBuilder qbRes = tested.handleCommonFiltersSettings(querySettings, qb);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_match_all.json", qbRes.toString());
		}

		// case - one project
		{
			filters.setProjects(Arrays.asList(new String[] { "pr1" }));
			QueryBuilder qb = QueryBuilders.matchAllQuery();
			QueryBuilder qbRes = tested.handleCommonFiltersSettings(querySettings, qb);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_filters_projects_one.json", qbRes.toString());
		}

		// case - more projects
		{
			filters.setProjects(Arrays.asList(new String[] { "pr1", "pr2", "pr3", "pr4" }));
			QueryBuilder qb = QueryBuilders.matchAllQuery();
			QueryBuilder qbRes = tested.handleCommonFiltersSettings(querySettings, qb);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_filters_projects_more.json", qbRes.toString());
		}
	}

	@Test
	public void handleCommonFiltersSettings_tags() throws IOException {
		SearchRestService tested = new SearchRestService();
		tested.log = Logger.getLogger("testlogger");

		QuerySettings querySettings = new QuerySettings();
		Filters filters = new Filters();
		querySettings.setFilters(filters);
		// case - list of tags is null
		{
			QueryBuilder qb = QueryBuilders.matchAllQuery();
			QueryBuilder qbRes = tested.handleCommonFiltersSettings(querySettings, qb);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_match_all.json", qbRes.toString());
		}

		// case - list of tags is empty
		{
			filters.setTags(Arrays.asList(new String[] {}));
			QueryBuilder qb = QueryBuilders.matchAllQuery();
			QueryBuilder qbRes = tested.handleCommonFiltersSettings(querySettings, qb);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_match_all.json", qbRes.toString());
		}

		// case - one tag
		{
			filters.setTags(Arrays.asList(new String[] { "tg1" }));
			QueryBuilder qb = QueryBuilders.matchAllQuery();
			QueryBuilder qbRes = tested.handleCommonFiltersSettings(querySettings, qb);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_filters_tags_one.json", qbRes.toString());
		}

		// case - more tags
		{
			filters.setTags(Arrays.asList(new String[] { "tg1", "tg2", "tg3", "tg4" }));
			QueryBuilder qb = QueryBuilders.matchAllQuery();
			QueryBuilder qbRes = tested.handleCommonFiltersSettings(querySettings, qb);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_filters_tags_more.json", qbRes.toString());
		}
	}

	@Test
	public void handleCommonFiltersSettings_contributors() throws IOException {
		SearchRestService tested = new SearchRestService();
		tested.log = Logger.getLogger("testlogger");

		QuerySettings querySettings = new QuerySettings();
		Filters filters = new Filters();
		querySettings.setFilters(filters);
		// case - list of contributors is null
		{
			QueryBuilder qb = QueryBuilders.matchAllQuery();
			QueryBuilder qbRes = tested.handleCommonFiltersSettings(querySettings, qb);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_match_all.json", qbRes.toString());
		}

		// case - list of contributors is empty
		{
			filters.setContributors(Arrays.asList(new String[] {}));
			QueryBuilder qb = QueryBuilders.matchAllQuery();
			QueryBuilder qbRes = tested.handleCommonFiltersSettings(querySettings, qb);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_match_all.json", qbRes.toString());
		}

		// case - one contributor
		{
			filters.setContributors(Arrays.asList(new String[] { "tg1" }));
			QueryBuilder qb = QueryBuilders.matchAllQuery();
			QueryBuilder qbRes = tested.handleCommonFiltersSettings(querySettings, qb);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_filters_contributors_one.json", qbRes.toString());
		}

		// case - more contributors
		{
			filters.setContributors(Arrays.asList(new String[] { "tg1", "tg2", "tg3", "tg4" }));
			QueryBuilder qb = QueryBuilders.matchAllQuery();
			QueryBuilder qbRes = tested.handleCommonFiltersSettings(querySettings, qb);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_filters_contributors_more.json", qbRes.toString());
		}
	}

	@Test
	public void handleFulltextSearchSettings() throws IOException {
		SearchRestService tested = new SearchRestService();
		tested.log = Logger.getLogger("testlogger");
		tested.configService = Mockito.mock(ConfigService.class);

		// case - NPE when no settings passed in
		try {
			tested.handleFulltextSearchSettings(null);
			Assert.fail("NullPointerException expected");
		} catch (NullPointerException e) {
			// OK
		}

		QuerySettings querySettings = new QuerySettings();

		// case - no fulltext parameter requested
		{
			querySettings.setQuery(null);
			QueryBuilder qbRes = tested.handleFulltextSearchSettings(querySettings);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_match_all.json", qbRes.toString());
		}

		// case - fulltext parameter requested, no fulltext fields configured
		{
			Mockito.reset(tested.configService);
			Mockito.when(tested.configService.get(ConfigService.CFGNAME_SEARCH_FULLTEXT_QUERY_FIELDS)).thenReturn(null);
			querySettings.setQuery("my query string");
			QueryBuilder qbRes = tested.handleFulltextSearchSettings(querySettings);
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
			QueryBuilder qbRes = tested.handleFulltextSearchSettings(querySettings);
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
			QueryBuilder qbRes = tested.handleFulltextSearchSettings(querySettings);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_match_all.json", qbRes.toString());
			Mockito.verifyZeroInteractions(tested.configService);
		}
	}

	@Test
	public void handleResponseContentSettings_pager() {
		SearchRestService tested = new SearchRestService();
		tested.log = Logger.getLogger("testlogger");
		tested.configService = Mockito.mock(ConfigService.class);

		SearchRequestBuilder srbMock = Mockito.mock(SearchRequestBuilder.class);

		// case - NPE when no settings passed in
		try {
			tested.handleResponseContentSettings(null, srbMock);
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
			tested.handleResponseContentSettings(querySettings, srbMock);
			Mockito.verifyNoMoreInteractions(srbMock);

			// empty filters object
			Mockito.reset(srbMock);
			querySettings.setFilters(new Filters());
			tested.handleResponseContentSettings(querySettings, srbMock);
			Mockito.verifyNoMoreInteractions(srbMock);
		}

		// case - size requested
		{
			Mockito.reset(srbMock);
			QuerySettings querySettings = new QuerySettings();
			querySettings.setFilters(new Filters());
			querySettings.getFilters().setSize(124);
			tested.handleResponseContentSettings(querySettings, srbMock);
			Mockito.verify(srbMock).setSize(124);
			Mockito.verifyNoMoreInteractions(srbMock);
		}

		// case - size requested but over maximum so stripped
		{
			Mockito.reset(srbMock);
			QuerySettings querySettings = new QuerySettings();
			querySettings.setFilters(new Filters());
			querySettings.getFilters().setSize(SearchRestService.RESPONSE_MAX_SIZE + 10);
			tested.handleResponseContentSettings(querySettings, srbMock);
			Mockito.verify(srbMock).setSize(SearchRestService.RESPONSE_MAX_SIZE);
			Mockito.verifyNoMoreInteractions(srbMock);
		}

		// case - size requested is 0
		{
			Mockito.reset(srbMock);
			QuerySettings querySettings = new QuerySettings();
			querySettings.setFilters(new Filters());
			querySettings.getFilters().setSize(0);
			tested.handleResponseContentSettings(querySettings, srbMock);
			Mockito.verify(srbMock).setSize(0);
			Mockito.verifyNoMoreInteractions(srbMock);
		}

		// case - size requested is under 0 so not used
		{
			Mockito.reset(srbMock);
			QuerySettings querySettings = new QuerySettings();
			querySettings.setFilters(new Filters());
			querySettings.getFilters().setSize(-1);
			tested.handleResponseContentSettings(querySettings, srbMock);
			Mockito.verifyNoMoreInteractions(srbMock);
		}

		// case - from requested
		{
			Mockito.reset(srbMock);
			QuerySettings querySettings = new QuerySettings();
			querySettings.setFilters(new Filters());
			querySettings.getFilters().setFrom(42);
			tested.handleResponseContentSettings(querySettings, srbMock);
			Mockito.verify(srbMock).setFrom(42);
			Mockito.verifyNoMoreInteractions(srbMock);
		}

		// case - from requested is 0
		{
			Mockito.reset(srbMock);
			QuerySettings querySettings = new QuerySettings();
			querySettings.setFilters(new Filters());
			querySettings.getFilters().setFrom(0);
			tested.handleResponseContentSettings(querySettings, srbMock);
			Mockito.verify(srbMock).setFrom(0);
			Mockito.verifyNoMoreInteractions(srbMock);
		}

		// case - from requested is under 0 so not used
		{
			Mockito.reset(srbMock);
			QuerySettings querySettings = new QuerySettings();
			querySettings.setFilters(new Filters());
			querySettings.getFilters().setFrom(-1);
			tested.handleResponseContentSettings(querySettings, srbMock);
			Mockito.verifyNoMoreInteractions(srbMock);
		}

		// case - size and from requested
		{
			Mockito.reset(srbMock);
			QuerySettings querySettings = new QuerySettings();
			querySettings.setFilters(new Filters());
			querySettings.getFilters().setSize(124);
			querySettings.getFilters().setFrom(42);
			tested.handleResponseContentSettings(querySettings, srbMock);
			Mockito.verify(srbMock).setSize(124);
			Mockito.verify(srbMock).setFrom(42);
			Mockito.verifyNoMoreInteractions(srbMock);
		}
	}

	@Test
	public void handleResponseContentSettings_fields() {
		SearchRestService tested = new SearchRestService();
		tested.log = Logger.getLogger("testlogger");
		tested.configService = Mockito.mock(ConfigService.class);

		SearchRequestBuilder srbMock = Mockito.mock(SearchRequestBuilder.class);

		// case - NPE when no settings passed in
		try {
			tested.handleResponseContentSettings(null, srbMock);
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
			tested.handleResponseContentSettings(querySettings, srbMock);
			Mockito.verify(tested.configService).get(ConfigService.CFGNAME_SEARCH_RESPONSE_FIELDS);
			Mockito.verifyNoMoreInteractions(srbMock);
			Mockito.verifyNoMoreInteractions(tested.configService);
		}

		// case - no fields requested so defaults loaded from configuration but do not contains correct key
		{
			// default is not defined
			Mockito.reset(srbMock, tested.configService);
			Map<String, Object> mockConfig = new HashMap<String, Object>();
			Mockito.when(tested.configService.get(ConfigService.CFGNAME_SEARCH_RESPONSE_FIELDS)).thenReturn(mockConfig);
			QuerySettings querySettings = new QuerySettings();
			tested.handleResponseContentSettings(querySettings, srbMock);
			Mockito.verify(tested.configService).get(ConfigService.CFGNAME_SEARCH_RESPONSE_FIELDS);
			Mockito.verifyNoMoreInteractions(srbMock);
			Mockito.verifyNoMoreInteractions(tested.configService);
		}

		// case - no fields requested so defaults loaded from configuration, contains correct key with String value
		{
			// default is not defined
			Mockito.reset(srbMock, tested.configService);
			Map<String, Object> mockConfig = new HashMap<String, Object>();
			mockConfig.put(ConfigService.CFGNAME_SEARCH_RESPONSE_FIELDS, "aa");
			Mockito.when(tested.configService.get(ConfigService.CFGNAME_SEARCH_RESPONSE_FIELDS)).thenReturn(mockConfig);
			QuerySettings querySettings = new QuerySettings();
			tested.handleResponseContentSettings(querySettings, srbMock);
			Mockito.verify(tested.configService).get(ConfigService.CFGNAME_SEARCH_RESPONSE_FIELDS);
			Mockito.verify(srbMock).addField("aa");
			Mockito.verifyNoMoreInteractions(srbMock);
			Mockito.verifyNoMoreInteractions(tested.configService);
		}

		// case - no fields requested so defaults loaded from configuration, contains correct key with List value
		{
			// default is not defined
			Mockito.reset(srbMock, tested.configService);
			Map<String, Object> mockConfig = new HashMap<String, Object>();
			List<String> cfgList = new ArrayList<String>();
			cfgList.add("bb");
			cfgList.add("cc");
			mockConfig.put(ConfigService.CFGNAME_SEARCH_RESPONSE_FIELDS, cfgList);
			Mockito.when(tested.configService.get(ConfigService.CFGNAME_SEARCH_RESPONSE_FIELDS)).thenReturn(mockConfig);
			QuerySettings querySettings = new QuerySettings();
			tested.handleResponseContentSettings(querySettings, srbMock);
			Mockito.verify(tested.configService).get(ConfigService.CFGNAME_SEARCH_RESPONSE_FIELDS);
			Mockito.verify(srbMock).addFields(new String[] { "bb", "cc" });
			Mockito.verifyNoMoreInteractions(srbMock);
			Mockito.verifyNoMoreInteractions(tested.configService);
		}

		// case - fields requested
		{
			Mockito.reset(srbMock, tested.configService);
			QuerySettings querySettings = new QuerySettings();
			List<String> fields = new ArrayList<String>();
			querySettings.setFields(fields);
			fields.add("aa");
			fields.add("bb");
			tested.handleResponseContentSettings(querySettings, srbMock);
			Mockito.verify(srbMock).addFields(new String[] { "aa", "bb" });
			Mockito.verifyNoMoreInteractions(srbMock);
			Mockito.verifyZeroInteractions(tested.configService);
		}
	}

	@Test
	public void handleSortingSettings() {
		SearchRestService tested = new SearchRestService();
		tested.log = Logger.getLogger("testlogger");

		SearchRequestBuilder srbMock = Mockito.mock(SearchRequestBuilder.class);

		// case - NPE when no settings passed in
		try {
			tested.handleSortingSettings(null, srbMock);
			Assert.fail("NullPointerException expected");
		} catch (NullPointerException e) {
			Mockito.verifyNoMoreInteractions(srbMock);
			// OK
		}

		// case - sorting not requested
		{
			Mockito.reset(srbMock);
			QuerySettings querySettings = new QuerySettings();
			tested.handleSortingSettings(querySettings, srbMock);
			Mockito.verifyNoMoreInteractions(srbMock);
		}

		// case - sorting requests
		{
			Mockito.reset(srbMock);
			QuerySettings querySettings = new QuerySettings();
			querySettings.setSortBy(SortByValue.NEW);
			tested.handleSortingSettings(querySettings, srbMock);
			Mockito.verify(srbMock).addSort("dcp_last_activity_date", SortOrder.DESC);
			Mockito.verifyNoMoreInteractions(srbMock);
		}
		{
			Mockito.reset(srbMock);
			QuerySettings querySettings = new QuerySettings();
			querySettings.setSortBy(SortByValue.OLD);
			tested.handleSortingSettings(querySettings, srbMock);
			Mockito.verify(srbMock).addSort("dcp_last_activity_date", SortOrder.ASC);
			Mockito.verifyNoMoreInteractions(srbMock);
		}

	}
}
