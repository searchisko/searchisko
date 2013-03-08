/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.common.settings.SettingsException;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.jboss.dcp.api.cache.IndexNamesCache;
import org.jboss.dcp.api.model.FacetValue;
import org.jboss.dcp.api.model.PastIntervalValue;
import org.jboss.dcp.api.model.QuerySettings;
import org.jboss.dcp.api.model.QuerySettings.Filters;
import org.jboss.dcp.api.model.SortByValue;
import org.jboss.dcp.api.testtools.TestUtils;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Unit test for {@link SearchService}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class SearchServiceTest {

	@Test
	public void prepareIndexNamesCacheKey() {

		Assert.assertEquals("_all||false", SearchService.prepareIndexNamesCacheKey(null, false));
		Assert.assertEquals("_all||true", SearchService.prepareIndexNamesCacheKey(null, true));
		List<String> dcpTypesRequested = new ArrayList<String>();
		Assert.assertEquals("_all||false", SearchService.prepareIndexNamesCacheKey(dcpTypesRequested, false));
		Assert.assertEquals("_all||true", SearchService.prepareIndexNamesCacheKey(dcpTypesRequested, true));

		dcpTypesRequested.add("aaaa");
		Assert.assertEquals("aaaa||true", SearchService.prepareIndexNamesCacheKey(dcpTypesRequested, true));
		Assert.assertEquals("aaaa||false", SearchService.prepareIndexNamesCacheKey(dcpTypesRequested, false));

		dcpTypesRequested.add("bb");
		Assert.assertEquals("aaaa|bb||true", SearchService.prepareIndexNamesCacheKey(dcpTypesRequested, true));
		Assert.assertEquals("aaaa|bb||false", SearchService.prepareIndexNamesCacheKey(dcpTypesRequested, false));

		// check ordering
		dcpTypesRequested = new ArrayList<String>();
		dcpTypesRequested.add("bb");
		dcpTypesRequested.add("zzzzz");
		dcpTypesRequested.add("aaaa");
		Assert.assertEquals("aaaa|bb|zzzzz||true", SearchService.prepareIndexNamesCacheKey(dcpTypesRequested, true));
		Assert.assertEquals("aaaa|bb|zzzzz||false", SearchService.prepareIndexNamesCacheKey(dcpTypesRequested, false));

	}

	@SuppressWarnings("unchecked")
	@Test
	public void handleSearchIndicesAndTypes() {
		SearchService tested = new SearchService();
		tested.providerService = Mockito.mock(ProviderService.class);
		tested.log = Logger.getLogger("testlogger");
		tested.indexNamesCache = Mockito.mock(IndexNamesCache.class);

		QuerySettings querySettings = new QuerySettings();
		SearchRequestBuilder searchRequestBuilderMock = Mockito.mock(SearchRequestBuilder.class);

		// case - searching for all types, no provider defined
		{
			Mockito.when(tested.indexNamesCache.get(Mockito.anyString())).thenReturn(null);
			List<Map<String, Object>> mockedProvidersList = new ArrayList<Map<String, Object>>();
			Mockito.when(tested.providerService.getAll()).thenReturn(mockedProvidersList);
			tested.handleSearchIndicesAndTypes(querySettings, searchRequestBuilderMock);
			Mockito.verify(tested.indexNamesCache).get("_all||false");
			Mockito.verify(tested.providerService).getAll();
			Mockito.verify(searchRequestBuilderMock).setIndices(new String[] {});
			Mockito.verifyNoMoreInteractions(searchRequestBuilderMock);
		}

		Filters filters = new Filters();
		querySettings.setFilters(filters);

		// case - searching for all types, some providers defined with all possible combinations of index definitions
		{
			Mockito.reset(tested.providerService, searchRequestBuilderMock, tested.indexNamesCache);
			Mockito.when(tested.indexNamesCache.get(Mockito.anyString())).thenReturn(null);
			List<Map<String, Object>> mockedProvidersList = new ArrayList<Map<String, Object>>();
			mockedProvidersList.add(TestUtils.loadJSONFromClasspathFile("/search/provider_1.json"));
			mockedProvidersList.add(TestUtils.loadJSONFromClasspathFile("/search/provider_2.json"));
			Mockito.when(tested.providerService.getAll()).thenReturn(mockedProvidersList);
			tested.handleSearchIndicesAndTypes(querySettings, searchRequestBuilderMock);
			Mockito.verify(tested.indexNamesCache).get("_all||false");
			Mockito.verify(tested.providerService).getAll();
			Mockito.verify(searchRequestBuilderMock).setIndices(
					new String[] { "idx_provider1_issue", "idx_provider1_mailing1", "idx_provider1_mailing2",
							"idx_provider2_mailing", "idx_provider2_issue1", "idx_provider2_issue2" });
			Mockito.verifyNoMoreInteractions(searchRequestBuilderMock);
		}

		// case - contentType filter used - type with one index
		{
			Mockito.reset(tested.providerService, searchRequestBuilderMock, tested.indexNamesCache);
			String testedType = "provider1_issue";
			filters.setContentType(testedType);
			Mockito.when(tested.providerService.findContentType(testedType)).thenReturn(
					((Map<String, Map<String, Object>>) TestUtils.loadJSONFromClasspathFile("/search/provider_1.json").get(
							ProviderService.TYPE)).get(testedType));
			tested.handleSearchIndicesAndTypes(querySettings, searchRequestBuilderMock);
			Mockito.verifyZeroInteractions(tested.indexNamesCache);
			Mockito.verify(searchRequestBuilderMock).setIndices(new String[] { "idx_provider1_issue" });
			Mockito.verify(searchRequestBuilderMock).setTypes(new String[] { "t_provider1_issue" });
			Mockito.verifyNoMoreInteractions(searchRequestBuilderMock);
		}

		// case - contentType filter used - type with more search indices
		{
			Mockito.reset(tested.providerService, searchRequestBuilderMock, tested.indexNamesCache);
			String testedType = "provider1_mailing";
			filters.setContentType(testedType);
			Mockito.when(tested.providerService.findContentType(testedType)).thenReturn(
					((Map<String, Map<String, Object>>) TestUtils.loadJSONFromClasspathFile("/search/provider_1.json").get(
							ProviderService.TYPE)).get(testedType));
			tested.handleSearchIndicesAndTypes(querySettings, searchRequestBuilderMock);
			Mockito.verifyZeroInteractions(tested.indexNamesCache);
			Mockito.verify(searchRequestBuilderMock).setIndices(
					new String[] { "idx_provider1_mailing1", "idx_provider1_mailing2" });
			Mockito.verify(searchRequestBuilderMock).setTypes(new String[] { "t_provider1_mailing" });
			Mockito.verifyNoMoreInteractions(searchRequestBuilderMock);
		}

		// case - contentType filter used - type with search_all_excluded=true can be used if named
		{
			Mockito.reset(tested.providerService, searchRequestBuilderMock, tested.indexNamesCache);
			String testedType = "provider1_cosi";
			filters.setContentType(testedType);
			Mockito.when(tested.providerService.findContentType(testedType)).thenReturn(
					((Map<String, Map<String, Object>>) TestUtils.loadJSONFromClasspathFile("/search/provider_1.json").get(
							ProviderService.TYPE)).get(testedType));
			tested.handleSearchIndicesAndTypes(querySettings, searchRequestBuilderMock);
			Mockito.verifyZeroInteractions(tested.indexNamesCache);
			Mockito.verify(searchRequestBuilderMock)
					.setIndices(new String[] { "idx_provider1_cosi1", "idx_provider1_cosi2" });
			Mockito.verify(searchRequestBuilderMock).setTypes(new String[] { "t_provider1_cosi" });
			Mockito.verifyNoMoreInteractions(searchRequestBuilderMock);
		}

		filters = new Filters();
		querySettings.setFilters(filters);
		// case - dcp_type filter used
		{
			Mockito.reset(tested.providerService, searchRequestBuilderMock, tested.indexNamesCache);
			Mockito.when(tested.indexNamesCache.get(Mockito.anyString())).thenReturn(null);
			List<String> dcpTypesRequested = Arrays.asList(new String[] { "issue" });
			filters.setDcpTypes(dcpTypesRequested);
			List<Map<String, Object>> mockedProvidersList = new ArrayList<Map<String, Object>>();
			mockedProvidersList.add(TestUtils.loadJSONFromClasspathFile("/search/provider_1.json"));
			mockedProvidersList.add(TestUtils.loadJSONFromClasspathFile("/search/provider_2.json"));
			Mockito.when(tested.providerService.getAll()).thenReturn(mockedProvidersList);
			tested.handleSearchIndicesAndTypes(querySettings, searchRequestBuilderMock);
			Mockito.verify(tested.indexNamesCache).get(SearchService.prepareIndexNamesCacheKey(dcpTypesRequested, false));
			Mockito.verify(tested.indexNamesCache).put(
					Mockito.eq(SearchService.prepareIndexNamesCacheKey(dcpTypesRequested, false)), Mockito.anySet());
			Mockito.verifyNoMoreInteractions(tested.indexNamesCache);
			Mockito.verify(tested.providerService).getAll();
			Mockito.verify(searchRequestBuilderMock).setIndices(
					new String[] { "idx_provider1_issue", "idx_provider2_issue1", "idx_provider2_issue2" });
			Mockito.verifyNoMoreInteractions(searchRequestBuilderMock);
		}

		// case - dcp_type filter used - type with search_all_excluded=true can be used if named
		{
			Mockito.reset(tested.providerService, searchRequestBuilderMock, tested.indexNamesCache);
			Mockito.when(tested.indexNamesCache.get(Mockito.anyString())).thenReturn(null);
			List<String> dcpTypesRequested = Arrays.asList(new String[] { "cosi" });
			filters.setDcpTypes(dcpTypesRequested);
			List<Map<String, Object>> mockedProvidersList = new ArrayList<Map<String, Object>>();
			mockedProvidersList.add(TestUtils.loadJSONFromClasspathFile("/search/provider_1.json"));
			mockedProvidersList.add(TestUtils.loadJSONFromClasspathFile("/search/provider_2.json"));
			Mockito.when(tested.providerService.getAll()).thenReturn(mockedProvidersList);
			tested.handleSearchIndicesAndTypes(querySettings, searchRequestBuilderMock);
			Mockito.verify(tested.indexNamesCache).get(SearchService.prepareIndexNamesCacheKey(dcpTypesRequested, false));
			Mockito.verify(tested.indexNamesCache).put(
					Mockito.eq(SearchService.prepareIndexNamesCacheKey(dcpTypesRequested, false)), Mockito.anySet());
			Mockito.verifyNoMoreInteractions(tested.indexNamesCache);
			Mockito.verify(tested.providerService).getAll();
			Mockito.verify(searchRequestBuilderMock).setIndices(
					new String[] { "idx_provider1_cosi1", "idx_provider1_cosi2", "idx_provider2_cosi1", "idx_provider2_cosi2" });
			Mockito.verifyNoMoreInteractions(searchRequestBuilderMock);
		}

		// case - dcp_type filter used with multiple values, no facet
		{
			Mockito.reset(tested.providerService, searchRequestBuilderMock, tested.indexNamesCache);
			Mockito.when(tested.indexNamesCache.get(Mockito.anyString())).thenReturn(null);
			List<String> dcpTypesRequested = Arrays.asList(new String[] { "issue", "cosi" });
			filters.setDcpTypes(dcpTypesRequested);
			List<Map<String, Object>> mockedProvidersList = new ArrayList<Map<String, Object>>();
			mockedProvidersList.add(TestUtils.loadJSONFromClasspathFile("/search/provider_1.json"));
			mockedProvidersList.add(TestUtils.loadJSONFromClasspathFile("/search/provider_2.json"));
			Mockito.when(tested.providerService.getAll()).thenReturn(mockedProvidersList);
			tested.handleSearchIndicesAndTypes(querySettings, searchRequestBuilderMock);
			Mockito.verify(tested.indexNamesCache).get(SearchService.prepareIndexNamesCacheKey(dcpTypesRequested, false));
			Mockito.verify(tested.indexNamesCache).put(
					Mockito.eq(SearchService.prepareIndexNamesCacheKey(dcpTypesRequested, false)), Mockito.anySet());
			Mockito.verifyNoMoreInteractions(tested.indexNamesCache);
			Mockito.verify(tested.providerService).getAll();
			Mockito.verify(searchRequestBuilderMock).setIndices(
					new String[] { "idx_provider1_cosi1", "idx_provider1_cosi2", "idx_provider1_issue", "idx_provider2_cosi1",
							"idx_provider2_cosi2", "idx_provider2_issue1", "idx_provider2_issue2" });
			Mockito.verifyNoMoreInteractions(searchRequestBuilderMock);
		}

		// case - dcp_type filter used with PER_DCP_TYPE_COUNTS facet. 'cosi' indexes are not included because
		// "search_all_excluded" : "true"
		{
			Mockito.reset(tested.providerService, searchRequestBuilderMock, tested.indexNamesCache);
			Mockito.when(tested.indexNamesCache.get(Mockito.anyString())).thenReturn(null);
			List<String> dcpTypesRequested = Arrays.asList(new String[] { "issue" });
			filters.setDcpTypes(dcpTypesRequested);
			querySettings.addFacet(FacetValue.PER_DCP_TYPE_COUNTS);
			List<Map<String, Object>> mockedProvidersList = new ArrayList<Map<String, Object>>();
			mockedProvidersList.add(TestUtils.loadJSONFromClasspathFile("/search/provider_1.json"));
			mockedProvidersList.add(TestUtils.loadJSONFromClasspathFile("/search/provider_2.json"));
			Mockito.when(tested.providerService.getAll()).thenReturn(mockedProvidersList);
			tested.handleSearchIndicesAndTypes(querySettings, searchRequestBuilderMock);
			Mockito.verify(tested.indexNamesCache).get(SearchService.prepareIndexNamesCacheKey(dcpTypesRequested, true));
			Mockito.verify(tested.indexNamesCache).put(
					Mockito.eq(SearchService.prepareIndexNamesCacheKey(dcpTypesRequested, true)), Mockito.anySet());
			Mockito.verifyNoMoreInteractions(tested.indexNamesCache);
			Mockito.verify(tested.providerService).getAll();
			Mockito.verify(searchRequestBuilderMock).setIndices(
					new String[] { "idx_provider1_issue", "idx_provider1_mailing1", "idx_provider1_mailing2",
							"idx_provider2_mailing", "idx_provider2_issue1", "idx_provider2_issue2" });
			Mockito.verifyNoMoreInteractions(searchRequestBuilderMock);
			querySettings.getFacets().clear();
		}

		// case - dcp_type filter used with PER_DCP_TYPE_COUNTS facet. 'cosi' indexes included even
		// "search_all_excluded" : "true" because requested
		{
			Mockito.reset(tested.providerService, searchRequestBuilderMock, tested.indexNamesCache);
			Mockito.when(tested.indexNamesCache.get(Mockito.anyString())).thenReturn(null);
			List<String> dcpTypesRequested = Arrays.asList(new String[] { "issue", "cosi" });
			filters.setDcpTypes(dcpTypesRequested);
			querySettings.addFacet(FacetValue.PER_DCP_TYPE_COUNTS);
			List<Map<String, Object>> mockedProvidersList = new ArrayList<Map<String, Object>>();
			mockedProvidersList.add(TestUtils.loadJSONFromClasspathFile("/search/provider_1.json"));
			mockedProvidersList.add(TestUtils.loadJSONFromClasspathFile("/search/provider_2.json"));
			Mockito.when(tested.providerService.getAll()).thenReturn(mockedProvidersList);
			tested.handleSearchIndicesAndTypes(querySettings, searchRequestBuilderMock);
			Mockito.verify(tested.indexNamesCache).get(SearchService.prepareIndexNamesCacheKey(dcpTypesRequested, true));
			Mockito.verify(tested.indexNamesCache).put(
					Mockito.eq(SearchService.prepareIndexNamesCacheKey(dcpTypesRequested, true)), Mockito.anySet());
			Mockito.verifyNoMoreInteractions(tested.indexNamesCache);
			Mockito.verify(tested.providerService).getAll();
			Mockito.verify(searchRequestBuilderMock).setIndices(
					new String[] { "idx_provider1_cosi1", "idx_provider1_cosi2", "idx_provider1_issue", "idx_provider1_mailing1",
							"idx_provider1_mailing2", "idx_provider2_cosi1", "idx_provider2_cosi2", "idx_provider2_mailing",
							"idx_provider2_issue1", "idx_provider2_issue2" });
			Mockito.verifyNoMoreInteractions(searchRequestBuilderMock);
			querySettings.getFacets().clear();
		}

		// case - dcp_type filter used with multiple values - cache hit
		{
			Mockito.reset(tested.providerService, searchRequestBuilderMock, tested.indexNamesCache);
			Set<String> cachedIdxNames = new HashSet<String>();
			cachedIdxNames.add("idx_provider1_cosi1");
			cachedIdxNames.add("idx_provider1_cosi2");
			cachedIdxNames.add("idx_provider1_issue");
			Mockito.when(tested.indexNamesCache.get(Mockito.anyString())).thenReturn(cachedIdxNames);
			List<String> dcpTypesRequested = Arrays.asList(new String[] { "issue", "cosi" });
			filters.setDcpTypes(dcpTypesRequested);
			List<Map<String, Object>> mockedProvidersList = new ArrayList<Map<String, Object>>();
			mockedProvidersList.add(TestUtils.loadJSONFromClasspathFile("/search/provider_1.json"));
			mockedProvidersList.add(TestUtils.loadJSONFromClasspathFile("/search/provider_2.json"));
			Mockito.when(tested.providerService.getAll()).thenReturn(mockedProvidersList);
			tested.handleSearchIndicesAndTypes(querySettings, searchRequestBuilderMock);

			Mockito.verify(tested.indexNamesCache).get(SearchService.prepareIndexNamesCacheKey(dcpTypesRequested, false));
			Mockito.verifyNoMoreInteractions(tested.indexNamesCache);
			Mockito.verifyZeroInteractions(tested.providerService);
			Mockito.verify(searchRequestBuilderMock).setIndices(
					new String[] { "idx_provider1_cosi1", "idx_provider1_cosi2", "idx_provider1_issue" });
			Mockito.verifyNoMoreInteractions(searchRequestBuilderMock);
		}

	}

	@Test
	public void handleCommonFiltersSettings_moreFilters() throws IOException {
		SearchService tested = new SearchService();
		tested.log = Logger.getLogger("testlogger");

		// case - NPE when no settings passed in
		try {
			tested.handleCommonFiltersSettings(null);
			Assert.fail("NullPointerException expected");
		} catch (NullPointerException e) {
			// OK
		}

		QuerySettings querySettings = new QuerySettings();

		// case - no filters object exists
		{
			QueryBuilder qb = QueryBuilders.matchAllQuery();
			QueryBuilder qbRes = tested.applyCommonFilters(tested.handleCommonFiltersSettings(querySettings), qb);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_match_all.json", qbRes.toString());
		}

		Filters filters = new Filters();
		querySettings.setFilters(filters);
		// case - empty filters object
		{
			QueryBuilder qb = QueryBuilders.matchAllQuery();
			QueryBuilder qbRes = tested.applyCommonFilters(tested.handleCommonFiltersSettings(querySettings), qb);
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
			QueryBuilder qbRes = tested.applyCommonFilters(tested.handleCommonFiltersSettings(querySettings), qb);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_filters_moreFilters.json", qbRes.toString());
		}
	}

	@Test
	public void handleCommonFiltersSettings_ActivityDateInterval() throws IOException {
		SearchService tested = new SearchService();
		tested.log = Logger.getLogger("testlogger");

		QuerySettings querySettings = new QuerySettings();
		Filters filters = new Filters();
		querySettings.setFilters(filters);
		filters.setActivityDateInterval(PastIntervalValue.TEST);
		// set date_from and date_to to some values to test this is ignored if interval is used
		filters.setActivityDateTo(1359232366456L);
		filters.setActivityDateFrom(1359232356456L);
		{
			QueryBuilder qb = QueryBuilders.matchAllQuery();
			QueryBuilder qbRes = tested.applyCommonFilters(tested.handleCommonFiltersSettings(querySettings), qb);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_filters_activityDateInterval.json", qbRes.toString());
		}
	}

	@Test
	public void handleCommonFiltersSettings_ActivityDateFromTo() throws IOException {
		SearchService tested = new SearchService();
		tested.log = Logger.getLogger("testlogger");

		QuerySettings querySettings = new QuerySettings();
		Filters filters = new Filters();
		querySettings.setFilters(filters);

		// case - only from
		filters.setActivityDateFrom(1359232356456L);
		filters.setActivityDateTo(null);
		{
			QueryBuilder qb = QueryBuilders.matchAllQuery();
			QueryBuilder qbRes = tested.applyCommonFilters(tested.handleCommonFiltersSettings(querySettings), qb);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_filters_activityDateFrom.json", qbRes.toString());
		}

		// case - only to
		filters.setActivityDateFrom(null);
		filters.setActivityDateTo(1359232366456L);
		{
			QueryBuilder qb = QueryBuilders.matchAllQuery();
			QueryBuilder qbRes = tested.applyCommonFilters(tested.handleCommonFiltersSettings(querySettings), qb);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_filters_activityDateTo.json", qbRes.toString());
		}

		// case - both bounds
		filters.setActivityDateFrom(1359232356456L);
		filters.setActivityDateTo(1359232366456L);
		{
			QueryBuilder qb = QueryBuilders.matchAllQuery();
			QueryBuilder qbRes = tested.applyCommonFilters(tested.handleCommonFiltersSettings(querySettings), qb);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_filters_activityDateFromTo.json", qbRes.toString());
		}
	}

	@Test
	public void handleCommonFiltersSettings_projects() throws IOException {
		SearchService tested = new SearchService();
		tested.log = Logger.getLogger("testlogger");

		QuerySettings querySettings = new QuerySettings();
		Filters filters = new Filters();
		querySettings.setFilters(filters);
		// case - list of projects is null
		{
			QueryBuilder qb = QueryBuilders.matchAllQuery();
			QueryBuilder qbRes = tested.applyCommonFilters(tested.handleCommonFiltersSettings(querySettings), qb);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_match_all.json", qbRes.toString());
		}

		// case - list of projects is empty
		{
			filters.setProjects(Arrays.asList(new String[] {}));
			QueryBuilder qb = QueryBuilders.matchAllQuery();
			QueryBuilder qbRes = tested.applyCommonFilters(tested.handleCommonFiltersSettings(querySettings), qb);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_match_all.json", qbRes.toString());
		}

		// case - one project
		{
			filters.setProjects(Arrays.asList(new String[] { "pr1" }));
			QueryBuilder qb = QueryBuilders.matchAllQuery();
			QueryBuilder qbRes = tested.applyCommonFilters(tested.handleCommonFiltersSettings(querySettings), qb);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_filters_projects_one.json", qbRes.toString());
		}

		// case - more projects
		{
			filters.setProjects(Arrays.asList(new String[] { "pr1", "pr2", "pr3", "pr4" }));
			QueryBuilder qb = QueryBuilders.matchAllQuery();
			QueryBuilder qbRes = tested.applyCommonFilters(tested.handleCommonFiltersSettings(querySettings), qb);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_filters_projects_more.json", qbRes.toString());
		}
	}

	@Test
	public void handleCommonFiltersSettings_tags() throws IOException {
		SearchService tested = new SearchService();
		tested.log = Logger.getLogger("testlogger");

		QuerySettings querySettings = new QuerySettings();
		Filters filters = new Filters();
		querySettings.setFilters(filters);
		// case - list of tags is null
		{
			QueryBuilder qb = QueryBuilders.matchAllQuery();
			QueryBuilder qbRes = tested.applyCommonFilters(tested.handleCommonFiltersSettings(querySettings), qb);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_match_all.json", qbRes.toString());
		}

		// case - list of tags is empty
		{
			filters.setTags(Arrays.asList(new String[] {}));
			QueryBuilder qb = QueryBuilders.matchAllQuery();
			QueryBuilder qbRes = tested.applyCommonFilters(tested.handleCommonFiltersSettings(querySettings), qb);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_match_all.json", qbRes.toString());
		}

		// case - one tag
		{
			filters.setTags(Arrays.asList(new String[] { "tg1" }));
			QueryBuilder qb = QueryBuilders.matchAllQuery();
			QueryBuilder qbRes = tested.applyCommonFilters(tested.handleCommonFiltersSettings(querySettings), qb);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_filters_tags_one.json", qbRes.toString());
		}

		// case - more tags
		{
			filters.setTags(Arrays.asList(new String[] { "tg1", "tg2", "tg3", "tg4" }));
			QueryBuilder qb = QueryBuilders.matchAllQuery();
			QueryBuilder qbRes = tested.applyCommonFilters(tested.handleCommonFiltersSettings(querySettings), qb);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_filters_tags_more.json", qbRes.toString());
		}
	}

	@Test
	public void handleCommonFiltersSettings_contributors() throws IOException {
		SearchService tested = new SearchService();
		tested.log = Logger.getLogger("testlogger");

		QuerySettings querySettings = new QuerySettings();
		Filters filters = new Filters();
		querySettings.setFilters(filters);
		// case - list of contributors is null
		{
			QueryBuilder qb = QueryBuilders.matchAllQuery();
			QueryBuilder qbRes = tested.applyCommonFilters(tested.handleCommonFiltersSettings(querySettings), qb);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_match_all.json", qbRes.toString());
		}

		// case - list of contributors is empty
		{
			filters.setContributors(Arrays.asList(new String[] {}));
			QueryBuilder qb = QueryBuilders.matchAllQuery();
			QueryBuilder qbRes = tested.applyCommonFilters(tested.handleCommonFiltersSettings(querySettings), qb);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_match_all.json", qbRes.toString());
		}

		// case - one contributor
		{
			filters.setContributors(Arrays.asList(new String[] { "tg1" }));
			QueryBuilder qb = QueryBuilders.matchAllQuery();
			QueryBuilder qbRes = tested.applyCommonFilters(tested.handleCommonFiltersSettings(querySettings), qb);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_filters_contributors_one.json", qbRes.toString());
		}

		// case - more contributors
		{
			filters.setContributors(Arrays.asList(new String[] { "tg1", "tg2", "tg3", "tg4" }));
			QueryBuilder qb = QueryBuilders.matchAllQuery();
			QueryBuilder qbRes = tested.applyCommonFilters(tested.handleCommonFiltersSettings(querySettings), qb);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_filters_contributors_more.json", qbRes.toString());
		}
	}

	@Test
	public void handleFulltextSearchSettings() throws IOException {
		SearchService tested = new SearchService();
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

	@SuppressWarnings("unchecked")
	@Test
	public void handleHighlightSettings() {
		SearchService tested = new SearchService();
		tested.log = Logger.getLogger("testlogger");
		tested.configService = Mockito.mock(ConfigService.class);

		SearchRequestBuilder srbMock = Mockito.mock(SearchRequestBuilder.class);

		// case - NPE when no settings passed in
		try {
			tested.handleHighlightSettings(null, srbMock);
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
			tested.handleHighlightSettings(querySettings, srbMock);
			Mockito.verifyZeroInteractions(srbMock);
			Mockito.verifyZeroInteractions(tested.configService);
		}

		// case - highlight requested not requested, fulltext query requested, nothing done
		{
			Mockito.reset(srbMock, tested.configService);
			querySettings.setQuery("query");
			querySettings.setQueryHighlight(false);
			tested.handleHighlightSettings(querySettings, srbMock);
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
			tested.handleHighlightSettings(querySettings, srbMock);
			Mockito.verify(srbMock).setHighlighterPreTags("<span class='hlt'>");
			Mockito.verify(srbMock).setHighlighterPostTags("</span>");
			Mockito.verify(srbMock).setHighlighterEncoder("html");
			Mockito.verify(srbMock).addHighlightedField("dcp_title", -1, 0, 0);
			Mockito.verify(srbMock).addHighlightedField("dcp_description", 2, 3, 20);
			Mockito.verify(srbMock).addHighlightedField("dcp_contributors.fulltext", 5, 10, 30);
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
				tested.handleHighlightSettings(querySettings, srbMock);
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
				tested.handleHighlightSettings(querySettings, srbMock);
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
			cfg.put("dcp_title", "badclass");
			Mockito.when(tested.configService.get(ConfigService.CFGNAME_SEARCH_FULLTEXT_HIGHLIGHT_FIELDS)).thenReturn(cfg);
			try {
				tested.handleHighlightSettings(querySettings, srbMock);
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
			Map<String, String> c = (Map<String, String>) cfg.get("dcp_title");
			// no integer parameter
			c.put("fragment_size", "no integer");
			Mockito.when(tested.configService.get(ConfigService.CFGNAME_SEARCH_FULLTEXT_HIGHLIGHT_FIELDS)).thenReturn(cfg);
			try {
				tested.handleHighlightSettings(querySettings, srbMock);
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
			Map<String, String> c = (Map<String, String>) cfg.get("dcp_title");
			// empty parameter
			c.put("number_of_fragments", "");
			Mockito.when(tested.configService.get(ConfigService.CFGNAME_SEARCH_FULLTEXT_HIGHLIGHT_FIELDS)).thenReturn(cfg);
			try {
				tested.handleHighlightSettings(querySettings, srbMock);
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
			Map<String, String> c = (Map<String, String>) cfg.get("dcp_title");
			// no integer parameter
			c.put("fragment_offset", "no integer");
			Mockito.when(tested.configService.get(ConfigService.CFGNAME_SEARCH_FULLTEXT_HIGHLIGHT_FIELDS)).thenReturn(cfg);
			try {
				tested.handleHighlightSettings(querySettings, srbMock);
				Assert.fail("SettingsException expected");
			} catch (SettingsException e) {
				// OK
			}
		}
	}

	@Test
	public void handleResponseContentSettings_pager() {
		SearchService tested = new SearchService();
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
			querySettings.getFilters().setSize(SearchService.RESPONSE_MAX_SIZE + 10);
			tested.handleResponseContentSettings(querySettings, srbMock);
			Mockito.verify(srbMock).setSize(SearchService.RESPONSE_MAX_SIZE);
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
		SearchService tested = new SearchService();
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
			querySettings.addField("aa");
			querySettings.addField("bb");
			tested.handleResponseContentSettings(querySettings, srbMock);
			Mockito.verify(srbMock).addFields(new String[] { "aa", "bb" });
			Mockito.verifyNoMoreInteractions(srbMock);
			Mockito.verifyZeroInteractions(tested.configService);
		}
	}

	@Test
	public void handleSortingSettings() {
		SearchService tested = new SearchService();
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
			querySettings.setSortBy(SortByValue.SCORE);
			tested.handleSortingSettings(querySettings, srbMock);
			Mockito.verifyNoMoreInteractions(srbMock);
		}
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
		{
			Mockito.reset(srbMock);
			QuerySettings querySettings = new QuerySettings();
			querySettings.setSortBy(SortByValue.NEW_CREATION);
			tested.handleSortingSettings(querySettings, srbMock);
			Mockito.verify(srbMock).addSort("dcp_created", SortOrder.DESC);
			Mockito.verifyNoMoreInteractions(srbMock);
		}
	}

	@Test
	public void getFilters() {
		{
			FilterBuilder[] ret = SearchService.getFilters(null, null);
			Assert.assertNotNull(ret);
			Assert.assertEquals(0, ret.length);
		}
		{
			FilterBuilder[] ret = SearchService.getFilters(null, "aaa");
			Assert.assertNotNull(ret);
			Assert.assertEquals(0, ret.length);
		}

		Map<String, FilterBuilder> filters = new LinkedHashMap<String, FilterBuilder>();
		FilterBuilder fb1 = Mockito.mock(FilterBuilder.class);
		filters.put("f1", fb1);
		FilterBuilder fb2 = Mockito.mock(FilterBuilder.class);
		filters.put("f2", fb2);
		FilterBuilder fb3 = Mockito.mock(FilterBuilder.class);
		filters.put("f3", fb3);
		{
			FilterBuilder[] ret = SearchService.getFilters(filters, null);
			Assert.assertNotNull(ret);
			Assert.assertEquals(3, ret.length);
			Assert.assertEquals(fb1, ret[0]);
			Assert.assertEquals(fb2, ret[1]);
			Assert.assertEquals(fb3, ret[2]);
		}

		{
			FilterBuilder[] ret = SearchService.getFilters(filters, "f2");
			Assert.assertNotNull(ret);
			Assert.assertEquals(2, ret.length);
			Assert.assertEquals(fb1, ret[0]);
			Assert.assertEquals(fb3, ret[1]);
		}
	}

	@Test
	public void handleFacetSettings_common() throws IOException {

		SearchService tested = new SearchService();
		tested.log = Logger.getLogger("testlogger");

		// case - no facets requested
		{
			SearchRequestBuilder srbMock = new SearchRequestBuilder(null);
			QuerySettings querySettings = new QuerySettings();
			tested.handleFacetSettings(querySettings, null, srbMock);
			Assert.assertEquals("{ }", srbMock.toString());
		}

		// case - one facet requested without filters
		{
			SearchRequestBuilder srbMock = new SearchRequestBuilder(null);
			QuerySettings querySettings = new QuerySettings();
			querySettings.addFacet(FacetValue.PER_DCP_TYPE_COUNTS);

			tested.handleFacetSettings(querySettings, null, srbMock);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_facets_per_dcp_type_counts.json", srbMock.toString());
		}

		// case - more facets requested without filters
		{
			SearchRequestBuilder srbMock = new SearchRequestBuilder(null);
			QuerySettings querySettings = new QuerySettings();
			querySettings.addFacet(FacetValue.ACTIVITY_DATES_HISTOGRAM);
			querySettings.addFacet(FacetValue.PER_PROJECT_COUNTS);
			querySettings.addFacet(FacetValue.TAG_CLOUD);
			querySettings.addFacet(FacetValue.TOP_CONTRIBUTORS);
			tested.handleFacetSettings(querySettings, null, srbMock);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_facets_moreNoFilter.json", srbMock.toString());
		}

		// case - more facets requested with more filters
		{
			SearchRequestBuilder srbMock = new SearchRequestBuilder(null);
			QuerySettings querySettings = new QuerySettings();
			Filters filters = new Filters();
			filters.setActivityDateFrom(100000l);
			filters.setActivityDateTo(100200l);
			filters.setContentType("my_content_type");
			filters.addContributor("my_contributor_1");
			filters.addContributor("my_contributor_2");
			filters.setDcpContentProvider("my_dcp_content_provider");
			filters.addDcpType("my_dcp_type");
			filters.addDcpType("my_dcp_type_2");
			filters.addProject("my_project");
			filters.addProject("my_project_2");
			filters.addTag("tag1");
			filters.addTag("tag2");
			querySettings.setFilters(filters);
			querySettings.addFacet(FacetValue.PER_DCP_TYPE_COUNTS);
			querySettings.addFacet(FacetValue.ACTIVITY_DATES_HISTOGRAM);
			querySettings.addFacet(FacetValue.PER_PROJECT_COUNTS);
			querySettings.addFacet(FacetValue.TAG_CLOUD);
			querySettings.addFacet(FacetValue.TOP_CONTRIBUTORS);
			tested.handleFacetSettings(querySettings, tested.handleCommonFiltersSettings(querySettings), srbMock);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_facets_moreWithFilter.json", srbMock.toString());
		}
	}

	@Test
	public void handleFacetSettings_top_contributors() throws IOException {

		SearchService tested = new SearchService();
		tested.log = Logger.getLogger("testlogger");

		// case - no contributor filter used, so only one top_contributors facet
		{
			SearchRequestBuilder srbMock = new SearchRequestBuilder(null);
			QuerySettings querySettings = new QuerySettings();
			querySettings.addFacet(FacetValue.TOP_CONTRIBUTORS);
			Filters filters = new Filters();
			filters.addTag("tag1");
			filters.addTag("tag2");
			querySettings.setFilters(filters);
			tested.handleFacetSettings(querySettings, tested.handleCommonFiltersSettings(querySettings), srbMock);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_facets_top_contributors_1.json", srbMock.toString());
		}

		// case - contributor filter used, so two top_contributors facets used
		{
			SearchRequestBuilder srbMock = new SearchRequestBuilder(null);
			QuerySettings querySettings = new QuerySettings();
			querySettings.addFacet(FacetValue.TOP_CONTRIBUTORS);
			Filters filters = new Filters();
			filters.addTag("tag1");
			filters.addTag("tag2");
			filters.addContributor("John Doe <john@doe.org>");
			querySettings.setFilters(filters);
			tested.handleFacetSettings(querySettings, tested.handleCommonFiltersSettings(querySettings), srbMock);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_facets_top_contributors_2.json", srbMock.toString());
		}
	}

	@Test
	public void handleFacetSettings_activity_dates_histogram() throws IOException {

		SearchService tested = new SearchService();
		tested.log = Logger.getLogger("testlogger");

		// case - no activity dates filter used
		{
			SearchRequestBuilder srbMock = new SearchRequestBuilder(null);
			QuerySettings querySettings = new QuerySettings();
			querySettings.addFacet(FacetValue.ACTIVITY_DATES_HISTOGRAM);
			Filters filters = new Filters();
			filters.addTag("tag1");
			filters.addTag("tag2");
			querySettings.setFilters(filters);
			tested.handleFacetSettings(querySettings, tested.handleCommonFiltersSettings(querySettings), srbMock);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_facets_activity_dates_histogram_1.json",
					srbMock.toString());
		}

		// case - activity dates interval filter used (so from/to is ignored)
		{
			SearchRequestBuilder srbMock = new SearchRequestBuilder(null);
			QuerySettings querySettings = new QuerySettings();
			querySettings.addFacet(FacetValue.ACTIVITY_DATES_HISTOGRAM);
			Filters filters = new Filters();
			filters.addTag("tag1");
			filters.addTag("tag2");
			filters.setActivityDateInterval(PastIntervalValue.TEST);
			filters.setActivityDateFrom(1256545l);
			filters.setActivityDateTo(2256545l);
			querySettings.setFilters(filters);
			tested.handleFacetSettings(querySettings, tested.handleCommonFiltersSettings(querySettings), srbMock);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_facets_activity_dates_histogram_2.json",
					srbMock.toString());
		}

		// case - activity dates from/to filter used
		{
			SearchRequestBuilder srbMock = new SearchRequestBuilder(null);
			QuerySettings querySettings = new QuerySettings();
			querySettings.addFacet(FacetValue.ACTIVITY_DATES_HISTOGRAM);
			Filters filters = new Filters();
			filters.addTag("tag1");
			filters.addTag("tag2");
			filters.setActivityDateFrom(1256545l);
			filters.setActivityDateTo(22256545l);
			querySettings.setFilters(filters);
			tested.handleFacetSettings(querySettings, tested.handleCommonFiltersSettings(querySettings), srbMock);
			TestUtils.assertJsonContentFromClasspathFile("/search/query_facets_activity_dates_histogram_3.json",
					srbMock.toString());
		}
	}

	@Test
	public void selectActivityDatesHistogramInterval_common() {

		// case - no activity dates filter defined
		QuerySettings qs = new QuerySettings();
		Assert.assertEquals("month", SearchService.selectActivityDatesHistogramInterval(qs));

		Filters filters = new Filters();
		qs.setFilters(filters);
		Assert.assertEquals("month", SearchService.selectActivityDatesHistogramInterval(qs));

		// case - activity date interval precedence against from/to
		filters.setActivityDateInterval(PastIntervalValue.YEAR);
		filters.setActivityDateFrom(System.currentTimeMillis() - 1000000);
		filters.setActivityDateTo(System.currentTimeMillis());
		Assert.assertEquals("week", SearchService.selectActivityDatesHistogramInterval(qs));
	}

	@Test
	public void selectActivityDatesHistogramInterval_intervalFilter() {
		QuerySettings qs = new QuerySettings();
		Filters filters = new Filters();
		qs.setFilters(filters);

		filters.setActivityDateInterval(PastIntervalValue.YEAR);
		Assert.assertEquals("week", SearchService.selectActivityDatesHistogramInterval(qs));
		filters.setActivityDateInterval(PastIntervalValue.QUARTER);
		Assert.assertEquals("week", SearchService.selectActivityDatesHistogramInterval(qs));

		filters.setActivityDateInterval(PastIntervalValue.MONTH);
		Assert.assertEquals("day", SearchService.selectActivityDatesHistogramInterval(qs));

		filters.setActivityDateInterval(PastIntervalValue.WEEK);
		Assert.assertEquals("day", SearchService.selectActivityDatesHistogramInterval(qs));

		filters.setActivityDateInterval(PastIntervalValue.DAY);
		Assert.assertEquals("hour", SearchService.selectActivityDatesHistogramInterval(qs));

	}

	@Test
	public void selectActivityDatesHistogramInterval_fromToFilter() {
		QuerySettings qs = new QuerySettings();
		Filters filters = new Filters();
		qs.setFilters(filters);

		// case - no from defined, so allways month
		filters.setActivityDateFrom(null);
		filters.setActivityDateTo(System.currentTimeMillis());
		Assert.assertEquals("month", SearchService.selectActivityDatesHistogramInterval(qs));

		filters.setActivityDateTo(System.currentTimeMillis() - 1000l * 60l * 60l * 24l * 365l * 10l);
		Assert.assertEquals("month", SearchService.selectActivityDatesHistogramInterval(qs));

		// case - no to defined means current timestamp is used
		filters.setActivityDateFrom(System.currentTimeMillis());
		filters.setActivityDateTo(null);
		Assert.assertEquals("minute", SearchService.selectActivityDatesHistogramInterval(qs));

		filters.setActivityDateFrom(System.currentTimeMillis() - 1000L * 60L * 60L + 100l);
		Assert.assertEquals("minute", SearchService.selectActivityDatesHistogramInterval(qs));
		filters.setActivityDateFrom(System.currentTimeMillis() - 1000L * 60L * 60L - 100l);
		Assert.assertEquals("hour", SearchService.selectActivityDatesHistogramInterval(qs));

		filters.setActivityDateFrom(System.currentTimeMillis() - 1000L * 60L * 60L * 24l * 2l + 100l);
		Assert.assertEquals("hour", SearchService.selectActivityDatesHistogramInterval(qs));
		filters.setActivityDateFrom(System.currentTimeMillis() - 1000L * 60L * 60L * 24l * 2l - 100l);
		Assert.assertEquals("day", SearchService.selectActivityDatesHistogramInterval(qs));

		filters.setActivityDateFrom(System.currentTimeMillis() - 1000L * 60L * 60L * 24l * 7l * 8l + 100l);
		Assert.assertEquals("day", SearchService.selectActivityDatesHistogramInterval(qs));
		filters.setActivityDateFrom(System.currentTimeMillis() - 1000L * 60L * 60L * 24l * 7l * 8l - 100l);
		Assert.assertEquals("week", SearchService.selectActivityDatesHistogramInterval(qs));

		filters.setActivityDateFrom(System.currentTimeMillis() - 1000L * 60L * 60L * 24l * 366l + 100l);
		Assert.assertEquals("week", SearchService.selectActivityDatesHistogramInterval(qs));
		filters.setActivityDateFrom(System.currentTimeMillis() - 1000L * 60L * 60L * 24l * 366l - 100l);
		Assert.assertEquals("month", SearchService.selectActivityDatesHistogramInterval(qs));

		// case - both from and to defined
		filters.setActivityDateFrom(1000l);
		filters.setActivityDateTo(1200l);
		Assert.assertEquals("minute", SearchService.selectActivityDatesHistogramInterval(qs));

		filters.setActivityDateTo(1000l + 1000L * 60L * 60L - 100l);
		Assert.assertEquals("minute", SearchService.selectActivityDatesHistogramInterval(qs));
		filters.setActivityDateTo(1000l + 1000L * 60L * 60L + 100l);
		Assert.assertEquals("hour", SearchService.selectActivityDatesHistogramInterval(qs));

		filters.setActivityDateFrom(1000000l);
		filters.setActivityDateTo(1000000l + 1000L * 60L * 60L * 24l * 2l - 100l);
		Assert.assertEquals("hour", SearchService.selectActivityDatesHistogramInterval(qs));
		filters.setActivityDateTo(1000000l + 1000L * 60L * 60L * 24l * 2l + 100l);
		Assert.assertEquals("day", SearchService.selectActivityDatesHistogramInterval(qs));

		filters.setActivityDateFrom(100000000l);
		filters.setActivityDateTo(100000000l + 1000L * 60L * 60L * 24l * 7l * 8l - 100l);
		Assert.assertEquals("day", SearchService.selectActivityDatesHistogramInterval(qs));
		filters.setActivityDateTo(100000000l + 1000L * 60L * 60L * 24l * 7l * 8l + 100l);
		Assert.assertEquals("week", SearchService.selectActivityDatesHistogramInterval(qs));

		filters.setActivityDateFrom(1000000000l);
		filters.setActivityDateTo(1000000000l + 1000L * 60L * 60L * 24l * 366l - 100l);
		Assert.assertEquals("week", SearchService.selectActivityDatesHistogramInterval(qs));
		filters.setActivityDateTo(1000000000l + 1000L * 60L * 60L * 24l * 366l + 100l);
		Assert.assertEquals("month", SearchService.selectActivityDatesHistogramInterval(qs));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void writeSearchHitUsedStatisticsRecord() {
		SearchService tested = new SearchService();
		tested.statsClientService = Mockito.mock(StatsClientService.class);
		tested.log = Logger.getLogger("test logger");

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
