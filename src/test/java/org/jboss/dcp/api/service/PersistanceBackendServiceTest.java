/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.service;

import java.util.Map;
import java.util.logging.Logger;

import junit.framework.Assert;

import org.elasticsearch.client.Client;
import org.jboss.dcp.api.model.AppConfiguration;
import org.jboss.dcp.api.testtools.ESRealClientTestBase;
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
	public void produceProviderService_doNotCreateProvider() {
		PersistanceBackendService tested = getTested();

		Mockito.reset(tested.appConfigurationService);
		AppConfiguration ac = new AppConfiguration();
		ac.setProviderCreateInitData(false);
		Mockito.when(tested.appConfigurationService.getAppConfiguration()).thenReturn(ac);
		EntityService s = tested.produceProviderService();
		assertElasticsearchEntityService(s, tested.client, EXPECTED_INDEX_NAME, "provider");

	}

	@Test
	public void produceProviderService_createProvider() {
		PersistanceBackendService tested = getTested();
		tested.client = prepareESClientForUnitTest();
		try {
			Mockito.reset(tested.appConfigurationService);
			AppConfiguration ac = new AppConfiguration();
			ac.setProviderCreateInitData(true);
			Mockito.when(tested.appConfigurationService.getAppConfiguration()).thenReturn(ac);
			EntityService s = tested.produceProviderService();

			assertElasticsearchEntityService(s, tested.client, EXPECTED_INDEX_NAME, "provider");
			indexFlush(PersistanceBackendService.INDEX_NAME);
			Map<String, Object> jborg = indexGetDocument(PersistanceBackendService.INDEX_NAME,
					PersistanceBackendService.INDEX_TYPE_PROVIDER, "jbossorg");
			Assert.assertNotNull(jborg);
			Assert.assertNotNull(jborg.get(ProviderService.PASSWORD_HASH));
			Assert.assertEquals(true, jborg.get(ProviderService.SUPER_PROVIDER));
			Assert.assertEquals("jbossorg", jborg.get(ProviderService.NAME));
		} finally {
			indexDelete(PersistanceBackendService.INDEX_NAME);
			finalizeESClientForUnitTest();
		}
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

	@Test
	public void produceConfigService() {
		PersistanceBackendService tested = getTested();
		EntityService s = tested.produceConfigService();
		assertElasticsearchEntityService(s, tested.client, EXPECTED_INDEX_NAME, "config");
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
