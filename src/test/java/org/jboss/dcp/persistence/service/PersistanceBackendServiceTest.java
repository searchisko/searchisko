/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.persistence.service;

import java.util.Map;
import java.util.logging.Logger;

import junit.framework.Assert;

import org.elasticsearch.client.Client;
import org.jboss.dcp.api.model.AppConfiguration;
import org.jboss.dcp.api.service.AppConfigurationService;
import org.jboss.dcp.api.service.ProviderService;
import org.jboss.dcp.api.testtools.ESRealClientTestBase;
import org.jboss.dcp.persistence.service.ElasticsearchEntityService;
import org.jboss.dcp.persistence.service.EntityService;
import org.jboss.dcp.persistence.service.PersistanceBackendService;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit test for {@link PersistanceBackendService}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class PersistanceBackendServiceTest extends ESRealClientTestBase {

	private final String EXPECTED_INDEX_NAME = "data";

	@Test
	public void init() {
		// untestable :-(
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
