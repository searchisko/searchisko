/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.elasticsearch.common.joda.time.format.DateTimeFormatter;
import org.elasticsearch.common.joda.time.format.ISODateTimeFormat;
import org.mockito.Mockito;
import org.searchisko.api.cache.IndexNamesCache;
import org.searchisko.api.testtools.TestUtils;

/**
 * Base class for Unit tests for {@link SearchService}. We have to divide them to moore subclasses to have source file
 * of reasonable size
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public abstract class SearchServiceTestBase {

	protected static final DateTimeFormatter DATE_TIME_FORMATTER_UTC = ISODateTimeFormat.dateTime().withZoneUTC();

	@SuppressWarnings("unchecked")
	protected void mockAuthenticatedUserWithRole(SearchService tested, String role) {
		Mockito.when(tested.authenticationUtilService.isAuthenticatedUser()).thenReturn(true);
		Mockito.when(tested.authenticationUtilService.isUserInAnyOfRoles(Mockito.anyBoolean(), Mockito.anyCollection()))
				.thenCallRealMethod();
		if (role != null)
			Mockito.when(tested.authenticationUtilService.isUserInRole(role)).thenReturn(true);
	}

	@SuppressWarnings("unchecked")
	protected void mockProviderConfiguration(SearchService tested, String... fileClassPaths) {
		List<Map<String, Object>> mockedProvidersList = new ArrayList<>();
		for (String s : fileClassPaths) {
			Map<String, Object> providerDef = TestUtils.loadJSONFromClasspathFile(s);
			mockedProvidersList.add(providerDef);

			// mock all types from provider definition
			Map<String, Object> types = (Map<String, Object>) providerDef.get(ProviderService.TYPE);
			if (types != null) {
				for (String testedType : types.keySet()) {
					Mockito.when(tested.providerService.findContentType(testedType)).thenReturn(
							ProviderServiceTest.createProviderContentTypeInfo(((Map<String, Object>) types.get(testedType))));
				}
			}

		}
		Mockito.when(tested.providerService.getAll()).thenReturn(mockedProvidersList);

	}

	protected ConfigService mockConfigurationService() {
		ConfigService configService = Mockito.mock(ConfigService.class);
		Map<String, Object> cfg = TestUtils.loadJSONFromClasspathFile("/search/search_fulltext_aggregations_fields.json");
		Mockito.when(configService.get(ConfigService.CFGNAME_SEARCH_FULLTEXT_AGGREGATIONS_FIELDS)).thenReturn(cfg);
		Map<String, Object> cfg2 = TestUtils.loadJSONFromClasspathFile("/search/search_fulltext_filter_fields.json");
		Mockito.when(configService.get(ConfigService.CFGNAME_SEARCH_FULLTEXT_FILTER_FIELDS)).thenReturn(cfg2);
		return configService;
	}

	protected SearchService getTested(ConfigService configService) {
		SearchService tested = new SearchService();
		tested.providerService = Mockito.mock(ProviderService.class);
		tested.log = Logger.getLogger("testlogger");
		tested.indexNamesCache = Mockito.mock(IndexNamesCache.class);
		if (configService != null) {
			tested.configService = configService;
			tested.parsedFilterConfigService = new ParsedFilterConfigService();
			tested.parsedFilterConfigService.log = Logger.getLogger("testloggercs");
			tested.parsedFilterConfigService.configService = configService;
		}
		tested.authenticationUtilService = Mockito.mock(AuthenticationUtilService.class);
		tested.statsClientService = Mockito.mock(StatsClientService.class);
		return tested;
	}

}