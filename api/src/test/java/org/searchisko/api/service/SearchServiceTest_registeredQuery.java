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
import org.searchisko.api.model.QuerySettings;
import org.searchisko.api.testtools.ESRealClientTestBase;
import org.searchisko.api.testtools.TestUtils;
import org.searchisko.persistence.service.EntityService;

import java.io.IOException;
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

    @Test
    public void testSearchServiceForRegisteredQuery() throws ReflectiveOperationException, IOException, JSONException {

        InnerESRealClient testHelper = new InnerESRealClient();

        try {
            SearchClientService searchClientService = testHelper.prepareSearchClientServiceMock("SearchServiceTest_registeredQuery");
            testHelper.indexCreate("idx_provider3_issue");
            testHelper.indexCreate("idx_provider3_mailing");
            testHelper.indexMappingCreate("idx_provider3_issue", "t_provider3_issue", "{ \"t_provider3_issue\": {}}");
            testHelper.indexMappingCreate("idx_provider3_mailing", "t_provider3_mailing", "{ \"t_provider3_mailing\": {}}");
            testHelper.indexInsertDocument("idx_provider3_issue", "t_provider3_issue", "1", "{\"field1\": \"value1\"}");
            testHelper.indexInsertDocument("idx_provider3_issue", "t_provider3_issue", "2", "{\"field1\": \"value2\"}");
            testHelper.indexFlushAndRefresh();

            ConfigService configService = mockConfigurationService();
            SearchService searchService = getTested(configService);
            mockProviderConfiguration(searchService, "/search/provider_3.json");

            // first create new registered query
            RegisteredQueryService registeredQueryService = getRegisteredQueryService(searchClientService);
            Map<String, Object> template = new HashMap<>();
            template.put("id", "template_1");
            template.put("description", "Match all query");
            template.put("template", "{ \"query\": { \"{{query_type}}\": {}}}");
            registeredQueryService.create("template_1", template);

            // search using registered query
            Map<String, Object> templateParams = new HashMap<>();
            templateParams.put("query_type", "match_all");

            // Basic match_all query test (including internal ES behaviour test)
            {
                QuerySettings.Filters filters = new QuerySettings.Filters();
                filters.acknowledgeUrlFilterCandidate("query_type", "match_all");
                searchService.parsedFilterConfigService.prepareFiltersForRequest(filters);

                SearchRequestBuilder srb = new SearchRequestBuilder(searchClientService.getClient());
                searchService.performSearchTemplateInternal("template_1", templateParams, filters, srb);

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
                Assert.assertEquals(2, response.getHits().getTotalHits());
                for (SearchHit hit : response.getHits().getHits()) {
                    // if setting fields (see [1]) were reflected then fields map could not have been empty
                    Assert.assertTrue(hit.fields().isEmpty());
                }
            }

            // Test using sys_type URL parameter
            {
                QuerySettings.Filters filters = new QuerySettings.Filters();
                filters.acknowledgeUrlFilterCandidate("query_type", "match_all");
                filters.acknowledgeUrlFilterCandidate("sys_type", "mailing");
                searchService.parsedFilterConfigService.prepareFiltersForRequest(filters);

                SearchRequestBuilder srb = new SearchRequestBuilder(searchClientService.getClient());
                searchService.performSearchTemplateInternal("template_1", templateParams, filters, srb);

                SearchResponse response = srb.get();
                Assert.assertEquals(0, response.getHits().getTotalHits());
            }

            // Test using type URL parameter
            {
                QuerySettings.Filters filters = new QuerySettings.Filters();
                filters.acknowledgeUrlFilterCandidate("query_type", "match_all");
                filters.acknowledgeUrlFilterCandidate("type", "provider3_mailing");
                searchService.parsedFilterConfigService.prepareFiltersForRequest(filters);

                SearchRequestBuilder srb = new SearchRequestBuilder(searchClientService.getClient());
                searchService.performSearchTemplateInternal("template_1", templateParams, filters, srb);

                SearchResponse response = srb.get();
                Assert.assertEquals(0, response.getHits().getTotalHits());
            }

            // Test using type and sys_type URL parameter
            {
                QuerySettings.Filters filters = new QuerySettings.Filters();
                filters.acknowledgeUrlFilterCandidate("query_type", "match_all");
                filters.acknowledgeUrlFilterCandidate("sys_type", "mailing");
                filters.acknowledgeUrlFilterCandidate("type", "provider3_issue");
                filters.acknowledgeUrlFilterCandidate("type", "provider3_mailing");
                searchService.parsedFilterConfigService.prepareFiltersForRequest(filters);

                SearchRequestBuilder srb = new SearchRequestBuilder(searchClientService.getClient());
                searchService.performSearchTemplateInternal("template_1", templateParams, filters, srb);

                SearchResponse response = srb.get();
                Assert.assertEquals(0, response.getHits().getTotalHits());
            }

        } finally {
            testHelper.indexDelete("idx_provider3_issue");
            testHelper.indexDelete("idx_provider3_mailing");
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
}
