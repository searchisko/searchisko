/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import java.util.logging.Logger;

import org.elasticsearch.client.Client;
import org.searchisko.api.testtools.ESRealClientTestBase;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit tests for {@link SearchService} against real embedded ElasticSearch client.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class SearchServiceTest_ESRealClient extends ESRealClientTestBase {

	@Test
	public void filterContributor() {
		Client client = prepareESClientForUnitTest();
		try {
			@SuppressWarnings("unused")
			SearchService tested = getTested(client);
			prepareTestData(client);

			// TODO _SEARCH unit test against real embedded ElasticSearch client.

		} finally {
			finalizeESClientForUnitTest();
		}
	}

	private void prepareTestData(Client client) {
		// TODO _SEARCH unit test against real embedded ElasticSearch client.
	}

	private SearchService getTested(Client client) {
		SearchService tested = new SearchService();
		tested.searchClientService = Mockito.mock(SearchClientService.class);
		Mockito.when(tested.searchClientService.getClient()).thenReturn(client);
		tested.providerService = Mockito.mock(ProviderService.class);
		tested.log = Logger.getLogger("testlogger");
		return tested;
	}

}
