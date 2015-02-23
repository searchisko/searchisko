/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import org.junit.Assert;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.json.JSONException;
import org.junit.Test;
import org.mockito.Mockito;
import org.searchisko.api.cache.RegisteredQueryCache;
import org.searchisko.api.model.QuerySettings;
import org.searchisko.api.testtools.ESRealClientTestBase;
import org.searchisko.api.testtools.TestUtils;
import org.searchisko.persistence.service.EntityService;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Unit test of {@link SearchService}'s registered query.
 *
 * @author Lukas Vlcek
 */

public class SearchServiceTest_registeredQuery extends SearchServiceTestBase {

    class InnerESRealClient extends ESRealClientTestBase {}

    private void prepareData(InnerESRealClient testHelper) {

        testHelper.indexCreate("idx_provider3_issue");
        testHelper.indexCreate("idx_provider3_mailing");
        testHelper.indexMappingCreate("idx_provider3_issue", "t_provider3_issue", "{ \"t_provider3_issue\": {}}");
        testHelper.indexMappingCreate("idx_provider3_mailing", "t_provider3_mailing", "{ \"t_provider3_mailing\": {}}");

        // index data
        testHelper.indexInsertDocument("idx_provider3_issue", "t_provider3_issue", "1", "{\"field1\": \"value1\"}");
        testHelper.indexInsertDocument("idx_provider3_issue", "t_provider3_issue", "2", "{\"field1\": \"value2\"}");
        testHelper.indexInsertDocument("idx_provider3_issue", "t_provider3_issue", "3", "{\"field1\": \"value3\"}");
        testHelper.indexInsertDocument("idx_provider3_mailing", "t_provider3_mailing", "1", "{\"field1\": \"value1\"}");
        testHelper.indexInsertDocument("idx_provider3_mailing", "t_provider3_mailing", "2", "{\"field1\": \"value2\"}");

        testHelper.indexFlushAndRefresh();
    }

    private void deleteDataAndIndices(InnerESRealClient testHelper) {
        testHelper.indexDelete("idx_provider3_issue");
        testHelper.indexDelete("idx_provider3_mailing");
    }

    @Test
    public void testRegisteredQueryMatchAllWithOptions() throws ReflectiveOperationException, IOException, JSONException {

        InnerESRealClient testHelper = new InnerESRealClient();

        try {
            SearchClientService searchClientService = testHelper.prepareSearchClientServiceMock("SearchServiceTest_registeredQuery[1]");
            prepareData(testHelper);

            ConfigService configService = mockConfigurationService();
            SearchService searchService = getTested(configService);
            mockProviderConfiguration(searchService, "/search/provider_3.json");

            RegisteredQueryService registeredQueryService = getRegisteredQueryService(searchClientService);
            searchService.registeredQueryService = registeredQueryService;

            // create registered query
            Map<String, Object> template = TestUtils.loadJSONFromClasspathFile("/registered_query/default_all_content.json");
            registeredQueryService.create("default_all_content", template);

            RegisteredQueryCache queryCache = Mockito.mock(RegisteredQueryCache.class);
            Mockito.when(queryCache.get("default_all_content")).thenReturn(template);

            registeredQueryService.registeredQueryCache = queryCache;

            // Basic match_all query test (including internal ES behaviour test)
            // also test the `default` level for `sys_content_type`.
            {
                Map<String, Object> templateParams = new HashMap<>();
                templateParams.put("query_type", "match_all");

                QuerySettings.Filters filters = new QuerySettings.Filters();
                filters.acknowledgeUrlFilterCandidate("query_type", "match_all");

                searchService.parsedFilterConfigService.prepareFiltersForRequest(filters);

                SearchRequestBuilder srb = new SearchRequestBuilder(searchClientService.getClient());
                searchService.performSearchTemplateInternal("default_all_content", templateParams, filters, srb);

                // This is to test Elasticsearch internal behaviour. It might be important to learn if it changes.
                // If SearchRequestBuilder (SRB) setTemplateName(), setTemplateType() and setTemplateParams()
                // are called it is not reflected in any way in .toString() method. Worse is that call to any other
                // methods on SRB after that is reflected in toString() but ignored during query execution.
                // For example if you pass SRB to some method then setting anything on SRB inside this method
                // may not have any effect if template has been already set on it.
                TestUtils.assertJsonContent("{}", srb.toString());

                // We set fields on the SRB but we will see later that this was not reflected in results.
                srb.addFields("field1"); // [1]
                TestUtils.assertJsonContent("{ \"fields\": \"field1\" }", srb.toString());

                SearchResponse response = srb.get();
                Assert.assertEquals(5, response.getHits().getTotalHits());
                for (SearchHit hit : response.getHits().getHits()) {
                    // if setting fields (see [1]) were reflected then fields map could not have been empty
                    Assert.assertTrue(hit.fields().isEmpty());
                }
            }

            // Test using sys_type URL parameter
            {
                Map<String, Object> templateParams = new HashMap<>();
                templateParams.put("query_type", "match_all");
                templateParams.put("sys_type", "mailing");

                QuerySettings.Filters filters = new QuerySettings.Filters();
                filters.acknowledgeUrlFilterCandidate("query_type", "match_all");
                filters.acknowledgeUrlFilterCandidate("sys_type", "mailing");

                searchService.parsedFilterConfigService.prepareFiltersForRequest(filters);

                SearchRequestBuilder srb = new SearchRequestBuilder(searchClientService.getClient());
                searchService.performSearchTemplateInternal("default_all_content", templateParams, filters, srb);

                SearchResponse response = srb.get();
                Assert.assertEquals(2, response.getHits().getTotalHits());
            }

            // Test using type URL parameter
            {
                Map<String, Object> templateParams = new HashMap<>();
                templateParams.put("query_type", "match_all");
                templateParams.put("type", "provider3_mailing");

                QuerySettings.Filters filters = new QuerySettings.Filters();
                filters.acknowledgeUrlFilterCandidate("query_type", "match_all");
                filters.acknowledgeUrlFilterCandidate("type", "provider3_mailing");
                searchService.parsedFilterConfigService.prepareFiltersForRequest(filters);

                SearchRequestBuilder srb = new SearchRequestBuilder(searchClientService.getClient());
                searchService.performSearchTemplateInternal("default_all_content", templateParams, filters, srb);

                SearchResponse response = srb.get();
                Assert.assertEquals(2, response.getHits().getTotalHits());
            }

            // Test using type and sys_type URL parameter
            {
                Map<String, Object> templateParams = new HashMap<>();
                templateParams.put("query_type", "match_all");
                templateParams.put("sys_type", "mailing");
                templateParams.put("type", Arrays.asList("provider3_issue", "provider3_mailing"));

                QuerySettings.Filters filters = new QuerySettings.Filters();
                filters.acknowledgeUrlFilterCandidate("query_type", "match_all");
                filters.acknowledgeUrlFilterCandidate("sys_type", "mailing");
                filters.acknowledgeUrlFilterCandidate("type", "provider3_issue", "provider3_mailing");
                searchService.parsedFilterConfigService.prepareFiltersForRequest(filters);

                SearchRequestBuilder srb = new SearchRequestBuilder(searchClientService.getClient());
                searchService.performSearchTemplateInternal("default_all_content", templateParams, filters, srb);

                SearchResponse response = srb.get();
                Assert.assertEquals(5, response.getHits().getTotalHits());
            }

        } finally {
            deleteDataAndIndices(testHelper);
            testHelper.finalizeESClientForUnitTest();
        }
    }

    @Test
    public void testRegisteredQuery_Default_SysContentType() throws ReflectiveOperationException, IOException, JSONException {

        InnerESRealClient testHelper = new InnerESRealClient();

        try {
            SearchClientService searchClientService = testHelper.prepareSearchClientServiceMock("SearchServiceTest_registeredQuery[2]");
            prepareData(testHelper);

            ConfigService configService = mockConfigurationService();
            SearchService searchService = getTested(configService);
            mockProviderConfiguration(searchService, "/search/provider_3.json");

            RegisteredQueryService registeredQueryService = getRegisteredQueryService(searchClientService);
            searchService.registeredQueryService = registeredQueryService;

            // create registered query
            Map<String, Object> template = TestUtils.loadJSONFromClasspathFile("/registered_query/default_sys_content_type.json");
            registeredQueryService.create("default_sys_content_type", template);

            RegisteredQueryCache queryCache = Mockito.mock(RegisteredQueryCache.class);
            Mockito.when(queryCache.get("default_sys_content_type")).thenReturn(template);

            registeredQueryService.registeredQueryCache = queryCache;

            {
                Map<String, Object> templateParams = new HashMap<>();
                templateParams.put("query_type", "match_all");

                QuerySettings.Filters filters = new QuerySettings.Filters();
                filters.acknowledgeUrlFilterCandidate("query_type", "match_all");

                searchService.parsedFilterConfigService.prepareFiltersForRequest(filters);

                SearchRequestBuilder srb = new SearchRequestBuilder(searchClientService.getClient());
                searchService.performSearchTemplateInternal("default_sys_content_type", templateParams, filters, srb);

                SearchResponse response = srb.get();
                Assert.assertEquals(3, response.getHits().getTotalHits());
            }

        } finally {
            deleteDataAndIndices(testHelper);
            testHelper.finalizeESClientForUnitTest();
        }
    }

    protected RegisteredQueryService getRegisteredQueryService(SearchClientService searchClientService) {
        RegisteredQueryService rqs = new RegisteredQueryService();
        rqs.searchClientService = searchClientService;
        rqs.searchClientService.log = Logger.getLogger("testlogger");
        rqs.entityService = Mockito.mock(EntityService.class);
        return rqs;
    }

    @Test
    public void testRegisteredQuery_Default_SysType() throws ReflectiveOperationException, IOException, JSONException {

        InnerESRealClient testHelper = new InnerESRealClient();

        try {
            SearchClientService searchClientService = testHelper.prepareSearchClientServiceMock("SearchServiceTest_registeredQuery[3]");
            prepareData(testHelper);

            ConfigService configService = mockConfigurationService();
            SearchService searchService = getTested(configService);
            mockProviderConfiguration(searchService, "/search/provider_3.json");

            RegisteredQueryService registeredQueryService = getRegisteredQueryService(searchClientService);
            searchService.registeredQueryService = registeredQueryService;

            // create registered query
            Map<String, Object> template = TestUtils.loadJSONFromClasspathFile("/registered_query/default_sys_type.json");
            registeredQueryService.create("default_sys_type", template);

            RegisteredQueryCache queryCache = Mockito.mock(RegisteredQueryCache.class);
            Mockito.when(queryCache.get("default_sys_type")).thenReturn(template);

            registeredQueryService.registeredQueryCache = queryCache;

            {
                Map<String, Object> templateParams = new HashMap<>();
                templateParams.put("query_type", "match_all");

                QuerySettings.Filters filters = new QuerySettings.Filters();
                filters.acknowledgeUrlFilterCandidate("query_type", "match_all");

                searchService.parsedFilterConfigService.prepareFiltersForRequest(filters);

                SearchRequestBuilder srb = new SearchRequestBuilder(searchClientService.getClient());
                searchService.performSearchTemplateInternal("default_sys_type", templateParams, filters, srb);

                SearchResponse response = srb.get();
                Assert.assertEquals(3, response.getHits().getTotalHits());
            }

        } finally {
            deleteDataAndIndices(testHelper);
            testHelper.finalizeESClientForUnitTest();
        }
    }
}
