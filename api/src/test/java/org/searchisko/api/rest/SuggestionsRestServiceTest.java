/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.elasticsearch.action.search.MultiSearchRequest;
import org.elasticsearch.action.search.MultiSearchRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.searchisko.api.service.SearchClientService;
import org.searchisko.api.testtools.TestUtils;

/**
 * Unit test for {@link SuggestionsRestService}
 * 
 * @author Lukas Vlcek
 */
public class SuggestionsRestServiceTest {

	@Test
	public void handleSuggestionsProject() throws IOException, JSONException {
		SuggestionsRestService tested = new SuggestionsRestService();
		tested.searchClientService = Mockito.mock(SearchClientService.class);
		tested.log = Logger.getLogger("testlogger");

		SearchRequestBuilder srbMock = new SearchRequestBuilder(null);
		srbMock = tested.getProjectSearchNGramRequestBuilder(srbMock, "JBoss", 5, null);

		TestUtils.assertJsonContentFromClasspathFile("/suggestions/project_suggestions_ngram.json", srbMock.toString());

		srbMock = new SearchRequestBuilder(null);
		List<String> customFields = new ArrayList<>();
		customFields.add("archived");
		customFields.add("license");
		customFields.add("projectName");
		srbMock = tested.getProjectSearchNGramRequestBuilder(srbMock, "JBoss", 5, customFields);

		TestUtils.assertJsonContentFromClasspathFile("/suggestions/project_suggestions_ngram_with_custom_fields.json", srbMock.toString());

		srbMock = new SearchRequestBuilder(null);
		srbMock = tested.getProjectSearchFuzzyRequestBuilder(srbMock, "JBoss", 5, null);

		TestUtils.assertJsonContentFromClasspathFile("/suggestions/project_suggestions_fuzzy.json", srbMock.toString());

		srbMock = new SearchRequestBuilder(null);
		customFields = new ArrayList<>();
		customFields.add("archived");
		customFields.add("license");
		customFields.add("projectName");
		srbMock = tested.getProjectSearchFuzzyRequestBuilder(srbMock, "JBoss", 5, customFields);

		TestUtils.assertJsonContentFromClasspathFile("/suggestions/project_suggestions_fuzzy_with_custom_fields.json", srbMock.toString());
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
				tested.getProjectSearchNGramRequestBuilder(srbNGram, "JBoss", 5, null),
				tested.getProjectSearchFuzzyRequestBuilder(srbFuzzy, "JBoss", 5, null));

		MultiSearchRequest msr = msrb.request();

		Assert.assertEquals(2, msr.requests().size());
	}
}
