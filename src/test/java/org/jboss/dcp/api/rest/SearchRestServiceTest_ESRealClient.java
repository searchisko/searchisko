/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.rest;

import java.util.logging.Logger;

import org.elasticsearch.client.Client;
import org.jboss.dcp.api.service.ProviderService;
import org.jboss.dcp.api.service.SearchClientService;
import org.jboss.dcp.api.testtools.ESRealClientTestBase;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit tests for {@link SearchRestService} against real embedded ElasticSearch client.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class SearchRestServiceTest_ESRealClient extends ESRealClientTestBase {

	@Test
	public void filterContributor() {
		Client client = prepareESClientForUnitTest();
		try {
			SearchRestService tested = getTested(client);
			prepareTestData(client);

			// TODO _SEARCH unit test against real embedded ElasticSearch client.

		} finally {
			finalizeESClientForUnitTest();
		}
	}

	private void prepareTestData(Client client) {
		// TODO Auto-generated method stub

	}

	private SearchRestService getTested(Client client) {
		SearchRestService tested = new SearchRestService();
		tested.searchClientService = Mockito.mock(SearchClientService.class);
		Mockito.when(tested.searchClientService.getClient()).thenReturn(client);
		tested.providerService = Mockito.mock(ProviderService.class);
		tested.log = Logger.getLogger("testlogger");
		return tested;
	}

}
