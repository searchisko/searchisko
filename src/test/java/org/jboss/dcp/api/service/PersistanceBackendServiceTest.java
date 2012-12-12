/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.service;

import java.util.logging.Logger;

import junit.framework.Assert;

import org.elasticsearch.client.Client;
import org.jboss.dcp.api.model.AppConfiguration;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit test for {@link PersistanceBackendService}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class PersistanceBackendServiceTest {

	private final String EXPECTED_INDEX_NAME = "data";

	@Test
	public void init() {
		// untestable :-(
	}

	@Test
	public void produceProviderService() {
		PersistanceBackendService tested = getTested();

		// case - with disabled default provider creation
		Mockito.reset(tested.appConfigurationService);
		Mockito.when(tested.appConfigurationService.getAppConfiguration()).thenReturn(new AppConfiguration());
		EntityService s = tested.produceProviderService();
		assertElasticsearchEntityService(s, tested.client, EXPECTED_INDEX_NAME, "provider");
	}

	@Test
	public void produceProjectService() {
		PersistanceBackendService tested = getTested();
		EntityService s = tested.produceProjectService();
		assertElasticsearchEntityService(s, tested.client, EXPECTED_INDEX_NAME, "project");
	}

	@Test
	public void produceContributorService() {
		PersistanceBackendService tested = getTested();
		EntityService s = tested.produceContributorService();
		assertElasticsearchEntityService(s, tested.client, EXPECTED_INDEX_NAME, "contributor");
	}

	private void assertElasticsearchEntityService(EntityService actualService, Client expectedClient,
			String expectedIndexName, String expectedIndexType) {
		Assert.assertEquals(ElasticsearchEntityService.class, actualService.getClass());
		ElasticsearchEntityService es = (ElasticsearchEntityService) actualService;
		Assert.assertEquals(expectedClient, es.client);
		Assert.assertEquals(expectedIndexName, es.indexName);
		Assert.assertEquals(expectedIndexType, es.indexType);
	}

	/**
	 * Prepare tested instance with injected mocks.
	 * 
	 * @return instance for test
	 */
	protected PersistanceBackendService getTested() {
		PersistanceBackendService tested = new PersistanceBackendService();

		tested.client = Mockito.mock(Client.class);
		tested.log = Logger.getLogger("testlogger");
		tested.appConfigurationService = Mockito.mock(AppConfigurationService.class);
		return tested;
	}
}
