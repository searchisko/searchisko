/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.common.settings.SettingsException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.searchisko.api.model.QuerySettings;
import org.searchisko.api.model.QuerySettings.Filters;
import org.searchisko.api.rest.exception.NotAuthorizedException;
import org.searchisko.api.security.Role;

/**
 * Unit test for {@link SearchService} setSearchRequestIndicesAndTypes() method.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 * @author Lukas Vlcek
 */
public class SearchServiceTest_setSearchRequestIndicesAndTypes extends SearchServiceTestBase {

	@SuppressWarnings("unchecked")
	@Test
	public void setSearchRequestIndicesAndTypes_basic() throws ReflectiveOperationException {
		ConfigService configService = mockConfigurationService();

		SearchService tested = getTested(configService);
		Mockito.when(tested.authenticationUtilService.isAuthenticatedUser()).thenReturn(false);

		QuerySettings querySettings = new QuerySettings();
		Filters filters = new Filters();
		querySettings.setFilters(filters);

		tested.parsedFilterConfigService.prepareFiltersForRequest(filters);

		SearchRequestBuilder searchRequestBuilderMock = Mockito.mock(SearchRequestBuilder.class);

		// case - searching for all types, no provider defined
		try {
			Mockito.when(tested.indexNamesCache.get(Mockito.anyString())).thenReturn(null);
			List<Map<String, Object>> mockedProvidersList = new ArrayList<>();
			Mockito.when(tested.providerService.getAll()).thenReturn(mockedProvidersList);
			tested.setSearchRequestIndicesAndTypes(querySettings, searchRequestBuilderMock);
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			Mockito.verify(tested.indexNamesCache).get("_all||false");
			Mockito.verify(tested.providerService).getAll();
			Mockito.verifyNoMoreInteractions(searchRequestBuilderMock);
		}

		// case - searching for all types, some providers defined with all possible combinations of index definitions
		{
			Mockito.reset(tested.providerService, searchRequestBuilderMock, tested.indexNamesCache);
			Mockito.when(tested.indexNamesCache.get(Mockito.anyString())).thenReturn(null);
			mockProviderConfiguration(tested, "/search/provider_1.json", "/search/provider_2.json");

			tested.setSearchRequestIndicesAndTypes(querySettings, searchRequestBuilderMock);
			Mockito.verify(tested.indexNamesCache).get("_all||false");
			Mockito.verify(tested.providerService).getAll();
			Mockito.verify(searchRequestBuilderMock).setIndices("idx_provider1_issue", "idx_provider1_mailing1",
					"idx_provider1_mailing2", "idx_provider2_issue1", "idx_provider2_issue2", "idx_provider2_mailing");
			Mockito.verify(tested.indexNamesCache).put(Mockito.eq("_all||false"), Mockito.anySet());
			Mockito.verifyNoMoreInteractions(searchRequestBuilderMock, tested.providerService, tested.indexNamesCache);
		}

		// case - searching for all types, cache used
		{
			Mockito.reset(tested.providerService, searchRequestBuilderMock, tested.indexNamesCache);
			Set<String> si = new HashSet<String>();
			si.add("idx_provider1_issue");
			Mockito.when(tested.indexNamesCache.get(Mockito.anyString())).thenReturn(si);

			tested.setSearchRequestIndicesAndTypes(querySettings, searchRequestBuilderMock);
			Mockito.verify(tested.indexNamesCache).get("_all||false");
			Mockito.verify(searchRequestBuilderMock).setIndices("idx_provider1_issue");
			Mockito.verifyNoMoreInteractions(searchRequestBuilderMock, tested.providerService, tested.indexNamesCache);
		}

	}

	@Test
	public void setSearchRequestIndicesAndTypes_contentTypeFilter() throws ReflectiveOperationException {
		ConfigService configService = mockConfigurationService();

		SearchService tested = getTested(configService);
		Mockito.when(tested.authenticationUtilService.isAuthenticatedUser()).thenReturn(false);

		QuerySettings querySettings = new QuerySettings();
		Filters filters = new Filters();
		querySettings.setFilters(filters);

		tested.parsedFilterConfigService.prepareFiltersForRequest(filters);

		SearchRequestBuilder searchRequestBuilderMock = Mockito.mock(SearchRequestBuilder.class);

		// case - contentType filter used - type with one index
		{
			Mockito.reset(tested.providerService, searchRequestBuilderMock, tested.indexNamesCache);
			mockProviderConfiguration(tested, "/search/provider_1.json", "/search/provider_2.json");

			filters.acknowledgeUrlFilterCandidate("type", "provider1_issue");
			tested.parsedFilterConfigService.prepareFiltersForRequest(filters);

			tested.setSearchRequestIndicesAndTypes(querySettings, searchRequestBuilderMock);
			Mockito.verifyZeroInteractions(tested.indexNamesCache);
			Mockito.verify(searchRequestBuilderMock).setIndices("idx_provider1_issue");
			Mockito.verify(searchRequestBuilderMock).setTypes("t_provider1_issue");
			Mockito.verifyNoMoreInteractions(searchRequestBuilderMock);
		}

		// case - contentType filter used - type with more search indices
		{
			Mockito.reset(tested.providerService, searchRequestBuilderMock, tested.indexNamesCache);
			mockProviderConfiguration(tested, "/search/provider_1.json", "/search/provider_2.json");

			filters.acknowledgeUrlFilterCandidate("type", "provider1_mailing");
			tested.parsedFilterConfigService.prepareFiltersForRequest(filters);

			tested.setSearchRequestIndicesAndTypes(querySettings, searchRequestBuilderMock);
			Mockito.verifyZeroInteractions(tested.indexNamesCache);
			Mockito.verify(searchRequestBuilderMock).setIndices("idx_provider1_mailing1", "idx_provider1_mailing2");
			Mockito.verify(searchRequestBuilderMock).setTypes("t_provider1_mailing");
			Mockito.verifyNoMoreInteractions(searchRequestBuilderMock);
		}

		// case - contentType filter used - type with search_all_excluded=true can be used if named
		{
			Mockito.reset(tested.providerService, searchRequestBuilderMock, tested.indexNamesCache);
			mockProviderConfiguration(tested, "/search/provider_1.json", "/search/provider_2.json");

			filters.acknowledgeUrlFilterCandidate("type", "provider1_cosi");
			tested.parsedFilterConfigService.prepareFiltersForRequest(filters);

			tested.setSearchRequestIndicesAndTypes(querySettings, searchRequestBuilderMock);
			Mockito.verifyZeroInteractions(tested.indexNamesCache);
			Mockito.verify(searchRequestBuilderMock).setIndices("idx_provider1_cosi1", "idx_provider1_cosi2");
			Mockito.verify(searchRequestBuilderMock).setTypes("t_provider1_cosi");
			Mockito.verifyNoMoreInteractions(searchRequestBuilderMock);
		}

		// case - contentType filter used - unknown type
		{
			Mockito.reset(tested.providerService, searchRequestBuilderMock, tested.indexNamesCache);
			mockProviderConfiguration(tested, "/search/provider_1.json", "/search/provider_2.json");

			filters.acknowledgeUrlFilterCandidate("type", "provider1_unknown");
			tested.parsedFilterConfigService.prepareFiltersForRequest(filters);

			try {
				tested.setSearchRequestIndicesAndTypes(querySettings, searchRequestBuilderMock);
				Assert.fail("IllegalArgumentException expected");
			} catch (IllegalArgumentException e) {
				Assert.assertEquals("Unsupported content type", e.getMessage());
				Mockito.verifyZeroInteractions(tested.indexNamesCache);
				Mockito.verifyNoMoreInteractions(searchRequestBuilderMock);
			}
		}

	}

	@SuppressWarnings("unchecked")
	@Test
	public void setSearchRequestIndicesAndTypes_sysTypeFilter() throws ReflectiveOperationException {
		ConfigService configService = mockConfigurationService();

		SearchService tested = getTested(configService);
		Mockito.when(tested.authenticationUtilService.isAuthenticatedUser()).thenReturn(false);

		QuerySettings querySettings = new QuerySettings();
		Filters filters = new Filters();
		querySettings.setFilters(filters);

		tested.parsedFilterConfigService.prepareFiltersForRequest(filters);

		SearchRequestBuilder searchRequestBuilderMock = Mockito.mock(SearchRequestBuilder.class);

		filters = new Filters();
		querySettings.setFilters(filters);
		// case - sys_type filter used
		{
			Mockito.reset(tested.providerService, searchRequestBuilderMock, tested.indexNamesCache);
			Mockito.when(tested.indexNamesCache.get(Mockito.anyString())).thenReturn(null);
			Set<String> sysTypesRequested = new HashSet<>();
			sysTypesRequested.add("issue");
			filters.acknowledgeUrlFilterCandidate("sys_type", new ArrayList<>(sysTypesRequested));
			tested.parsedFilterConfigService.prepareFiltersForRequest(filters);

			mockProviderConfiguration(tested, "/search/provider_1.json", "/search/provider_2.json");

			tested.setSearchRequestIndicesAndTypes(querySettings, searchRequestBuilderMock);
			Mockito.verify(tested.indexNamesCache).get(SearchService.prepareIndexNamesCacheKey(sysTypesRequested, false));
			Mockito.verify(tested.indexNamesCache).put(
					Mockito.eq(SearchService.prepareIndexNamesCacheKey(sysTypesRequested, false)), Mockito.anySet());
			Mockito.verifyNoMoreInteractions(tested.indexNamesCache);
			Mockito.verify(tested.providerService).getAll();
			Mockito.verify(searchRequestBuilderMock).setIndices("idx_provider1_issue", "idx_provider2_issue1",
					"idx_provider2_issue2");
			Mockito.verifyNoMoreInteractions(searchRequestBuilderMock);
		}

		// case - sys_type filter used but unknown
		{
			Mockito.reset(tested.providerService, searchRequestBuilderMock, tested.indexNamesCache);
			Mockito.when(tested.indexNamesCache.get(Mockito.anyString())).thenReturn(null);
			Set<String> sysTypesRequested = new HashSet<>();
			sysTypesRequested.add("unknowntype");
			filters.acknowledgeUrlFilterCandidate("sys_type", new ArrayList<>(sysTypesRequested));
			tested.parsedFilterConfigService.prepareFiltersForRequest(filters);

			mockProviderConfiguration(tested, "/search/provider_1.json", "/search/provider_2.json");
			try {
				tested.setSearchRequestIndicesAndTypes(querySettings, searchRequestBuilderMock);
				Assert.fail("IllegalArgumentException expected");
			} catch (IllegalArgumentException e) {
				Assert.assertEquals("Unsupported content sys_type", e.getMessage());
				Mockito.verify(tested.indexNamesCache).get(SearchService.prepareIndexNamesCacheKey(sysTypesRequested, false));
				Mockito.verify(tested.providerService).getAll();
				Mockito.verifyNoMoreInteractions(searchRequestBuilderMock, tested.providerService, tested.indexNamesCache);
			}
		}

		// case - sys_type filter used - type with search_all_excluded=true can be used if named
		{
			Mockito.reset(tested.providerService, searchRequestBuilderMock, tested.indexNamesCache);
			Mockito.when(tested.indexNamesCache.get(Mockito.anyString())).thenReturn(null);
			Set<String> sysTypesRequested = new HashSet<>();
			sysTypesRequested.add("cosi");
			filters.acknowledgeUrlFilterCandidate("sys_type", new ArrayList<>(sysTypesRequested));
			tested.parsedFilterConfigService.prepareFiltersForRequest(filters);
			mockProviderConfiguration(tested, "/search/provider_1.json", "/search/provider_2.json");

			tested.setSearchRequestIndicesAndTypes(querySettings, searchRequestBuilderMock);
			Mockito.verify(tested.indexNamesCache).get(SearchService.prepareIndexNamesCacheKey(sysTypesRequested, false));
			Mockito.verify(tested.indexNamesCache).put(
					Mockito.eq(SearchService.prepareIndexNamesCacheKey(sysTypesRequested, false)), Mockito.anySet());
			Mockito.verifyNoMoreInteractions(tested.indexNamesCache);
			Mockito.verify(tested.providerService).getAll();
			Mockito.verify(searchRequestBuilderMock).setIndices("idx_provider1_cosi1", "idx_provider1_cosi2",
					"idx_provider2_cosi1", "idx_provider2_cosi2");
			Mockito.verifyNoMoreInteractions(searchRequestBuilderMock);
		}

		// case - sys_type filter used with multiple values, no aggregation
		{
			Mockito.reset(tested.providerService, searchRequestBuilderMock, tested.indexNamesCache);
			Mockito.when(tested.indexNamesCache.get(Mockito.anyString())).thenReturn(null);
			Set<String> sysTypesRequested = new HashSet<>();
			sysTypesRequested.add("issue");
			sysTypesRequested.add("cosi");
			filters.acknowledgeUrlFilterCandidate("sys_type", new ArrayList<>(sysTypesRequested));
			tested.parsedFilterConfigService.prepareFiltersForRequest(filters);
			mockProviderConfiguration(tested, "/search/provider_1.json", "/search/provider_2.json");

			tested.setSearchRequestIndicesAndTypes(querySettings, searchRequestBuilderMock);
			Mockito.verify(tested.indexNamesCache).get(SearchService.prepareIndexNamesCacheKey(sysTypesRequested, false));
			Mockito.verify(tested.indexNamesCache).put(
					Mockito.eq(SearchService.prepareIndexNamesCacheKey(sysTypesRequested, false)), Mockito.anySet());
			Mockito.verifyNoMoreInteractions(tested.indexNamesCache);
			Mockito.verify(tested.providerService).getAll();
			Mockito.verify(searchRequestBuilderMock).setIndices("idx_provider1_issue", "idx_provider1_cosi1",
					"idx_provider1_cosi2", "idx_provider2_issue1", "idx_provider2_issue2", "idx_provider2_cosi1",
					"idx_provider2_cosi2");
			Mockito.verifyNoMoreInteractions(searchRequestBuilderMock);
		}

		// case - sys_type filter used with PER_SYS_TYPE_COUNTS aggregation. 'cosi' indexes are not included because
		// "search_all_excluded" : "true"
		{
			Mockito.reset(tested.providerService, searchRequestBuilderMock, tested.indexNamesCache);
			Mockito.when(tested.indexNamesCache.get(Mockito.anyString())).thenReturn(null);
			Set<String> sysTypesRequested = new HashSet<>();
			sysTypesRequested.add("issue");
			filters.acknowledgeUrlFilterCandidate("sys_type", new ArrayList<>(sysTypesRequested));
			tested.parsedFilterConfigService.prepareFiltersForRequest(filters);
			querySettings.addAggregation("per_sys_type_counts");
			mockProviderConfiguration(tested, "/search/provider_1.json", "/search/provider_2.json");

			tested.setSearchRequestIndicesAndTypes(querySettings, searchRequestBuilderMock);
			Mockito.verify(tested.indexNamesCache).get(SearchService.prepareIndexNamesCacheKey(sysTypesRequested, true));
			Mockito.verify(tested.indexNamesCache).put(
					Mockito.eq(SearchService.prepareIndexNamesCacheKey(sysTypesRequested, true)), Mockito.anySet());
			Mockito.verifyNoMoreInteractions(tested.indexNamesCache);
			Mockito.verify(tested.providerService).getAll();
			Mockito.verify(searchRequestBuilderMock).setIndices("idx_provider1_issue", "idx_provider1_mailing1",
					"idx_provider1_mailing2", "idx_provider2_issue1", "idx_provider2_issue2", "idx_provider2_mailing");
			Mockito.verifyNoMoreInteractions(searchRequestBuilderMock);
			querySettings.getAggregations().clear();
		}

		// case - sys_type filter used with PER_SYS_TYPE_COUNTS aggregation. 'cosi' indexes included even
		// "search_all_excluded" : "true" because requested
		{
			Mockito.reset(tested.providerService, searchRequestBuilderMock, tested.indexNamesCache);
			Mockito.when(tested.indexNamesCache.get(Mockito.anyString())).thenReturn(null);
			Set<String> sysTypesRequested = new HashSet<>();
			sysTypesRequested.add("issue");
			sysTypesRequested.add("cosi");
			filters.acknowledgeUrlFilterCandidate("sys_type", new ArrayList<>(sysTypesRequested));
			tested.parsedFilterConfigService.prepareFiltersForRequest(filters);
			querySettings.addAggregation("per_sys_type_counts");
			mockProviderConfiguration(tested, "/search/provider_1.json", "/search/provider_2.json");

			tested.setSearchRequestIndicesAndTypes(querySettings, searchRequestBuilderMock);
			Mockito.verify(tested.indexNamesCache).get(SearchService.prepareIndexNamesCacheKey(sysTypesRequested, true));
			Mockito.verify(tested.indexNamesCache).put(
					Mockito.eq(SearchService.prepareIndexNamesCacheKey(sysTypesRequested, true)), Mockito.anySet());
			Mockito.verifyNoMoreInteractions(tested.indexNamesCache);
			Mockito.verify(tested.providerService).getAll();
			Mockito.verify(searchRequestBuilderMock).setIndices("idx_provider1_issue", "idx_provider1_mailing1",
					"idx_provider1_mailing2", "idx_provider1_cosi1", "idx_provider1_cosi2", "idx_provider2_issue1",
					"idx_provider2_issue2", "idx_provider2_mailing", "idx_provider2_cosi1", "idx_provider2_cosi2");
			Mockito.verifyNoMoreInteractions(searchRequestBuilderMock);
			querySettings.getAggregations().clear();
		}

		// case - sys_type filter used with multiple values - cache hit
		{
			Mockito.reset(tested.providerService, searchRequestBuilderMock, tested.indexNamesCache);
			Set<String> cachedIdxNames = new HashSet<>();
			cachedIdxNames.add("idx_provider1_cosi1");
			cachedIdxNames.add("idx_provider1_cosi2");
			cachedIdxNames.add("idx_provider1_issue");
			Mockito.when(tested.indexNamesCache.get(Mockito.anyString())).thenReturn(cachedIdxNames);
			Set<String> sysTypesRequested = new HashSet<>();
			sysTypesRequested.add("issue");
			sysTypesRequested.add("cosi");
			filters.acknowledgeUrlFilterCandidate("sys_type", new ArrayList<>(sysTypesRequested));
			tested.parsedFilterConfigService.prepareFiltersForRequest(filters);
			mockProviderConfiguration(tested, "/search/provider_1.json", "/search/provider_2.json");

			tested.setSearchRequestIndicesAndTypes(querySettings, searchRequestBuilderMock);

			Mockito.verify(tested.indexNamesCache).get(SearchService.prepareIndexNamesCacheKey(sysTypesRequested, false));
			Mockito.verifyNoMoreInteractions(tested.indexNamesCache);
			Mockito.verifyZeroInteractions(tested.providerService);
			Mockito.verify(searchRequestBuilderMock).setIndices("idx_provider1_cosi1", "idx_provider1_cosi2",
					"idx_provider1_issue");
			Mockito.verifyNoMoreInteractions(searchRequestBuilderMock);
		}
	}

	@Test
	public void setSearchRequestIndicesAndTypes_security_basic() throws ReflectiveOperationException {
		ConfigService configService = mockConfigurationService();

		SearchService tested = getTested(configService);

		QuerySettings querySettings = new QuerySettings();
		Filters filters = new Filters();
		querySettings.setFilters(filters);

		tested.parsedFilterConfigService.prepareFiltersForRequest(filters);

		SearchRequestBuilder searchRequestBuilderMock = Mockito.mock(SearchRequestBuilder.class);

		// case - searching for all types, some providers defined with all possible combinations of index definitions, no
		// any role for any additional type
		{
			Mockito.reset(tested.providerService, searchRequestBuilderMock, tested.indexNamesCache,
					tested.authenticationUtilService);
			mockProviderConfiguration(tested, "/search/provider_1.json", "/search/provider_2.json");
			mockAuthenticatedUserWithRole(tested, null);

			tested.setSearchRequestIndicesAndTypes(querySettings, searchRequestBuilderMock);

			Mockito.verify(tested.providerService).getAll();
			Mockito.verify(searchRequestBuilderMock).setIndices("idx_provider1_issue", "idx_provider1_mailing1",
					"idx_provider1_mailing2", "idx_provider2_issue1", "idx_provider2_issue2", "idx_provider2_mailing");
			// NO CACHE USED !!!
			Mockito.verifyNoMoreInteractions(searchRequestBuilderMock, tested.providerService, tested.indexNamesCache);
		}

		// case - user has some role but not correct one to grant additional index
		{
			Mockito.reset(tested.providerService, searchRequestBuilderMock, tested.indexNamesCache,
					tested.authenticationUtilService);
			mockAuthenticatedUserWithRole(tested, "ROLE2");

			mockProviderConfiguration(tested, "/search/provider_1.json", "/search/provider_2.json");

			tested.setSearchRequestIndicesAndTypes(querySettings, searchRequestBuilderMock);

			Mockito.verify(tested.providerService).getAll();
			Mockito.verify(searchRequestBuilderMock).setIndices("idx_provider1_issue", "idx_provider1_mailing1",
					"idx_provider1_mailing2", "idx_provider2_issue1", "idx_provider2_issue2", "idx_provider2_mailing");
			// NO CACHE USED !!!
			Mockito.verifyNoMoreInteractions(searchRequestBuilderMock, tested.providerService, tested.indexNamesCache);
		}

		// case - user has some role to grant additional index
		{
			Mockito.reset(tested.providerService, searchRequestBuilderMock, tested.indexNamesCache,
					tested.authenticationUtilService);
			mockAuthenticatedUserWithRole(tested, "ROLE1");

			mockProviderConfiguration(tested, "/search/provider_1.json", "/search/provider_2.json");

			tested.setSearchRequestIndicesAndTypes(querySettings, searchRequestBuilderMock);

			Mockito.verify(tested.providerService).getAll();
			Mockito.verify(searchRequestBuilderMock).setIndices("idx_provider1_issue", "idx_provider1_mailing1",
					"idx_provider1_mailing2", "idx_provider1_issue_secure", "idx_provider2_issue1", "idx_provider2_issue2",
					"idx_provider2_mailing");
			// NO CACHE USED !!!
			Mockito.verifyNoMoreInteractions(searchRequestBuilderMock, tested.providerService, tested.indexNamesCache);
		}

		// case - ADMIN has granted additional index
		{
			Mockito.reset(tested.providerService, searchRequestBuilderMock, tested.indexNamesCache,
					tested.authenticationUtilService);
			mockAuthenticatedUserWithRole(tested, Role.ADMIN);

			mockProviderConfiguration(tested, "/search/provider_1.json", "/search/provider_2.json");

			tested.setSearchRequestIndicesAndTypes(querySettings, searchRequestBuilderMock);

			Mockito.verify(tested.providerService).getAll();
			Mockito.verify(searchRequestBuilderMock).setIndices("idx_provider1_issue", "idx_provider1_mailing1",
					"idx_provider1_mailing2", "idx_provider1_issue_secure", "idx_provider2_issue1", "idx_provider2_issue2",
					"idx_provider2_mailing");
			// NO CACHE USED !!!
			Mockito.verifyNoMoreInteractions(searchRequestBuilderMock, tested.providerService, tested.indexNamesCache);
		}

	}

	@Test
	public void setSearchRequestIndicesAndTypes_security_contentTypeFilter() throws ReflectiveOperationException {
		ConfigService configService = mockConfigurationService();

		SearchService tested = getTested(configService);

		QuerySettings querySettings = new QuerySettings();
		Filters filters = new Filters();
		querySettings.setFilters(filters);

		tested.parsedFilterConfigService.prepareFiltersForRequest(filters);

		SearchRequestBuilder searchRequestBuilderMock = Mockito.mock(SearchRequestBuilder.class);

		// case - contentType filter used - user has no necessary role
		{
			Mockito.reset(tested.providerService, searchRequestBuilderMock, tested.authenticationUtilService);
			mockAuthenticatedUserWithRole(tested, "ROLE2");
			mockProviderConfiguration(tested, "/search/provider_1.json", "/search/provider_2.json");

			filters.acknowledgeUrlFilterCandidate("type", "provider1_issue_secure");
			tested.parsedFilterConfigService.prepareFiltersForRequest(filters);

			try {
				tested.setSearchRequestIndicesAndTypes(querySettings, searchRequestBuilderMock);
				Assert.fail("NotAuthorizedException expected");
			} catch (NotAuthorizedException e) {
				Mockito.verifyNoMoreInteractions(searchRequestBuilderMock, tested.indexNamesCache);
			}
		}
		{
			Mockito.reset(tested.providerService, searchRequestBuilderMock, tested.authenticationUtilService);
			mockAuthenticatedUserWithRole(tested, "ROLE2");
			mockProviderConfiguration(tested, "/search/provider_1.json", "/search/provider_2.json");

			filters.acknowledgeUrlFilterCandidate("type", "provider2_issue_secure");
			tested.parsedFilterConfigService.prepareFiltersForRequest(filters);

			try {
				tested.setSearchRequestIndicesAndTypes(querySettings, searchRequestBuilderMock);
				Assert.fail("NotAuthorizedException expected");
			} catch (NotAuthorizedException e) {
				Mockito.verifyNoMoreInteractions(searchRequestBuilderMock, tested.indexNamesCache);
			}
		}

		// case - contentType filter used - user has necessary role
		{
			Mockito.reset(tested.providerService, searchRequestBuilderMock, tested.indexNamesCache,
					tested.authenticationUtilService);
			mockAuthenticatedUserWithRole(tested, "ROLE1");
			mockProviderConfiguration(tested, "/search/provider_1.json", "/search/provider_2.json");

			filters.acknowledgeUrlFilterCandidate("type", "provider1_issue_secure");
			tested.parsedFilterConfigService.prepareFiltersForRequest(filters);

			tested.setSearchRequestIndicesAndTypes(querySettings, searchRequestBuilderMock);

			Mockito.verify(searchRequestBuilderMock).setIndices("idx_provider1_issue_secure");
			Mockito.verify(searchRequestBuilderMock).setTypes("t_provider1_issue_secure");
			Mockito.verifyNoMoreInteractions(searchRequestBuilderMock, tested.indexNamesCache);
		}

		// case - contentType filter used - type with search_all_excluded=true can be used if named
		{
			Mockito.reset(tested.providerService, searchRequestBuilderMock, tested.indexNamesCache,
					tested.authenticationUtilService);
			mockProviderConfiguration(tested, "/search/provider_1.json", "/search/provider_2.json");
			mockAuthenticatedUserWithRole(tested, "ROLE1");

			filters.acknowledgeUrlFilterCandidate("type", "provider2_issue_secure");
			tested.parsedFilterConfigService.prepareFiltersForRequest(filters);

			tested.setSearchRequestIndicesAndTypes(querySettings, searchRequestBuilderMock);

			Mockito.verify(searchRequestBuilderMock).setIndices("idx_provider2_issue_secure");
			Mockito.verify(searchRequestBuilderMock).setTypes("t_provider2_issue_secure");
			Mockito.verifyNoMoreInteractions(searchRequestBuilderMock, tested.indexNamesCache);
		}

		// case - contentType filter used - ADMIN has right always
		{
			Mockito.reset(tested.providerService, searchRequestBuilderMock, tested.indexNamesCache,
					tested.authenticationUtilService);
			mockAuthenticatedUserWithRole(tested, Role.ADMIN);
			mockProviderConfiguration(tested, "/search/provider_1.json", "/search/provider_2.json");

			filters.acknowledgeUrlFilterCandidate("type", "provider1_issue_secure");
			tested.parsedFilterConfigService.prepareFiltersForRequest(filters);

			tested.setSearchRequestIndicesAndTypes(querySettings, searchRequestBuilderMock);

			Mockito.verify(searchRequestBuilderMock).setIndices("idx_provider1_issue_secure");
			Mockito.verify(searchRequestBuilderMock).setTypes("t_provider1_issue_secure");
			Mockito.verifyNoMoreInteractions(searchRequestBuilderMock, tested.indexNamesCache);
		}

		// case - contentType filter used - unknown type
		{
			Mockito.reset(tested.providerService, searchRequestBuilderMock, tested.indexNamesCache,
					tested.authenticationUtilService);
			mockProviderConfiguration(tested, "/search/provider_1.json", "/search/provider_2.json");
			mockAuthenticatedUserWithRole(tested, "ROLE1");

			filters.acknowledgeUrlFilterCandidate("type", "provider1_unknown");
			tested.parsedFilterConfigService.prepareFiltersForRequest(filters);

			try {
				tested.setSearchRequestIndicesAndTypes(querySettings, searchRequestBuilderMock);
				Assert.fail("IllegalArgumentException expected");
			} catch (IllegalArgumentException e) {
				Assert.assertEquals("Unsupported content type", e.getMessage());
				Mockito.verifyZeroInteractions(tested.indexNamesCache);
				Mockito.verifyNoMoreInteractions(searchRequestBuilderMock);
			}
		}
	}

	@Test
	public void setSearchRequestIndicesAndTypes_security_sysTypeFilter() throws ReflectiveOperationException {
		ConfigService configService = mockConfigurationService();

		SearchService tested = getTested(configService);

		QuerySettings querySettings = new QuerySettings();
		Filters filters = new Filters();
		querySettings.setFilters(filters);

		tested.parsedFilterConfigService.prepareFiltersForRequest(filters);

		SearchRequestBuilder searchRequestBuilderMock = Mockito.mock(SearchRequestBuilder.class);

		filters = new Filters();
		querySettings.setFilters(filters);

		// case - sys_type filter used - no permission to additional indexes
		{
			Mockito.reset(tested.providerService, searchRequestBuilderMock, tested.authenticationUtilService);
			Mockito.when(tested.indexNamesCache.get(Mockito.anyString())).thenReturn(null);
			Set<String> sysTypesRequested = new HashSet<>();
			sysTypesRequested.add("issue");
			filters.acknowledgeUrlFilterCandidate("sys_type", new ArrayList<>(sysTypesRequested));
			tested.parsedFilterConfigService.prepareFiltersForRequest(filters);

			mockProviderConfiguration(tested, "/search/provider_1.json", "/search/provider_2.json");
			mockAuthenticatedUserWithRole(tested, "ROLE2");

			tested.setSearchRequestIndicesAndTypes(querySettings, searchRequestBuilderMock);

			Mockito.verify(tested.providerService).getAll();
			Mockito.verify(searchRequestBuilderMock).setIndices("idx_provider1_issue", "idx_provider2_issue1",
					"idx_provider2_issue2");
			// NO CACHE!!!
			Mockito.verifyNoMoreInteractions(searchRequestBuilderMock, tested.indexNamesCache);
		}

		// case - sys_type filter used - role with permission to additional indexes
		{
			Mockito.reset(tested.providerService, searchRequestBuilderMock, tested.authenticationUtilService);
			Mockito.when(tested.indexNamesCache.get(Mockito.anyString())).thenReturn(null);
			Set<String> sysTypesRequested = new HashSet<>();
			sysTypesRequested.add("issue");
			filters.acknowledgeUrlFilterCandidate("sys_type", new ArrayList<>(sysTypesRequested));
			tested.parsedFilterConfigService.prepareFiltersForRequest(filters);

			mockProviderConfiguration(tested, "/search/provider_1.json", "/search/provider_2.json");
			mockAuthenticatedUserWithRole(tested, "ROLE1");

			tested.setSearchRequestIndicesAndTypes(querySettings, searchRequestBuilderMock);

			Mockito.verify(tested.providerService).getAll();
			Mockito.verify(searchRequestBuilderMock).setIndices("idx_provider1_issue", "idx_provider1_issue_secure",
					"idx_provider2_issue1", "idx_provider2_issue2", "idx_provider2_issue_secure");
			// NO CACHE!!!
			Mockito.verifyNoMoreInteractions(searchRequestBuilderMock, tested.indexNamesCache);
		}

		// case - sys_type filter used - ADMIN has always permission to additional indexes
		{
			Mockito.reset(tested.providerService, searchRequestBuilderMock, tested.authenticationUtilService);
			Mockito.when(tested.indexNamesCache.get(Mockito.anyString())).thenReturn(null);
			Set<String> sysTypesRequested = new HashSet<>();
			sysTypesRequested.add("issue");
			filters.acknowledgeUrlFilterCandidate("sys_type", new ArrayList<>(sysTypesRequested));
			tested.parsedFilterConfigService.prepareFiltersForRequest(filters);

			mockProviderConfiguration(tested, "/search/provider_1.json", "/search/provider_2.json");
			mockAuthenticatedUserWithRole(tested, Role.ADMIN);

			tested.setSearchRequestIndicesAndTypes(querySettings, searchRequestBuilderMock);

			Mockito.verify(tested.providerService).getAll();
			Mockito.verify(searchRequestBuilderMock).setIndices("idx_provider1_issue", "idx_provider1_issue_secure",
					"idx_provider2_issue1", "idx_provider2_issue2", "idx_provider2_issue_secure");
			// NO CACHE!!!
			Mockito.verifyNoMoreInteractions(searchRequestBuilderMock, tested.indexNamesCache);
		}

		// case - sys_type filter used but unknown
		{
			Mockito.reset(tested.providerService, searchRequestBuilderMock, tested.indexNamesCache);
			Mockito.when(tested.indexNamesCache.get(Mockito.anyString())).thenReturn(null);
			Set<String> sysTypesRequested = new HashSet<>();
			sysTypesRequested.add("unknowntype");
			filters.acknowledgeUrlFilterCandidate("sys_type", new ArrayList<>(sysTypesRequested));
			tested.parsedFilterConfigService.prepareFiltersForRequest(filters);

			mockProviderConfiguration(tested, "/search/provider_1.json", "/search/provider_2.json");
			mockAuthenticatedUserWithRole(tested, Role.ADMIN);

			try {
				tested.setSearchRequestIndicesAndTypes(querySettings, searchRequestBuilderMock);
				Assert.fail("IllegalArgumentException expected");
			} catch (IllegalArgumentException e) {
				Assert.assertEquals("Unsupported content sys_type", e.getMessage());
				Mockito.verify(tested.providerService).getAll();
				// NO CACHE!!!
				Mockito.verifyNoMoreInteractions(searchRequestBuilderMock, tested.providerService, tested.indexNamesCache);
			}
		}

	}

}
