/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.rest;

import junit.framework.Assert;
import org.elasticsearch.action.search.MultiSearchRequest;
import org.elasticsearch.action.search.MultiSearchRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.jboss.dcp.api.service.SearchClientService;
import org.jboss.dcp.api.testtools.TestUtils;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Unit test for {@link SuggestionsRestService}
 *
 * @author Lukas Vlcek
 */
public class SuggestionsRestServiceTest {

    @Test
    public void handleSuggestionsProject() throws IOException {
        SuggestionsRestService tested = new SuggestionsRestService();
        tested.searchClientService = Mockito.mock(SearchClientService.class);
        tested.log = Logger.getLogger("testlogger");

        SearchRequestBuilder srbMock = new SearchRequestBuilder(null);
        srbMock = tested.getProjectSearchNGramRequestBuilder(srbMock, "JBoss", 5);

        TestUtils.assertJsonContentFromClasspathFile("/suggestions/project_suggestions_ngram.json", srbMock.toString());

        srbMock = new SearchRequestBuilder(null);
        srbMock = tested.getProjectSearchFuzzyRequestBuilder(srbMock, "JBoss", 5);

        TestUtils.assertJsonContentFromClasspathFile("/suggestions/project_suggestions_fuzzy.json", srbMock.toString());
    }

    @Test
    public void testMultiSearchRequestBuilder() throws IOException {
        SuggestionsRestService tested = new SuggestionsRestService();
        tested.searchClientService = Mockito.mock(SearchClientService.class);
        tested.log = Logger.getLogger("testlogger");

        MultiSearchRequestBuilder msrb = new MultiSearchRequestBuilder(null);
        SearchRequestBuilder srbNGram = new SearchRequestBuilder(null);
        SearchRequestBuilder srbFuzzy = new SearchRequestBuilder(null);

        msrb = tested.getProjectMultiSearchRequestBuilder(msrb,
                tested.getProjectSearchNGramRequestBuilder(srbNGram, "JBoss", 5),
                tested.getProjectSearchFuzzyRequestBuilder(srbFuzzy, "JBoss", 5));

        MultiSearchRequest msr = msrb.request();

        Assert.assertEquals(2, msr.requests().size());
    }
}
