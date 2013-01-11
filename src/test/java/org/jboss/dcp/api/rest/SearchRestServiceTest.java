/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.core.UriInfo;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.jboss.dcp.api.model.QuerySettings;
import org.jboss.dcp.api.model.QuerySettings.Filters;
import org.jboss.dcp.api.service.ProviderService;
import org.jboss.dcp.api.testtools.TestUtils;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * TODO Unit test for {@link SearchRestService}
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

		// case - type filter used - type with one index
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

		// case - type filter used - type with more search indices
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

		// case - type filter used - type with search_all_excluded=true can be used if named
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

	}
}
