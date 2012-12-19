/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.dcp.api.util.SearchUtils;

/**
 * Service for persistence backend.<br/>
 * Now persistence storage is implemented as embedded elasticsearch node
 * 
 * @author Libor Krzyzanek
 * 
 */
@Named
@Singleton
@ApplicationScoped
@Startup
public class PersistanceBackendService extends ElasticsearchClientService {

	@Inject
	protected AppConfigurationService appConfigurationService;

	// Everything is stored in one index
	protected static final String INDEX_NAME = "data";
	protected static final String INDEX_TYPE_PROVIDER = "provider";

	@PostConstruct
	public void init() throws Exception {
		Properties settings = SearchUtils.loadProperties("/persistance_settings.properties");

		node = createEmbeddedNode("persistance", settings);
		client = node.client();
	}

	@Produces
	@Named("providerServiceBackend")
	public ElasticsearchEntityService produceProviderService() {
		ElasticsearchEntityService serv = new ElasticsearchEntityService(client, INDEX_NAME, INDEX_TYPE_PROVIDER, false);

		if (appConfigurationService.getAppConfiguration().isProviderCreateInitData()) {

			if (!client.admin().indices().prepareExists(INDEX_NAME).execute().actionGet().exists()) {
				log.info("Provider entity doesn't exists. Creating initial entity for first authentication.");

				client.admin().indices().prepareCreate(INDEX_NAME).execute().actionGet();
				Map<String, Object> jbossorgEntity = new HashMap<String, Object>();
				jbossorgEntity.put(ProviderService.NAME, "jbossorg");
				jbossorgEntity.put(ProviderService.PASSWORD_HASH, "0228a71fe1a4cf2f8a177cad2f165f5a4e021af6");
				jbossorgEntity.put(ProviderService.SUPER_PROVIDER, true);

				serv.create("jbossorg", jbossorgEntity);
			}
		}

		return serv;
	}

	@Produces
	@Named("projectServiceBackend")
	public EntityService produceProjectService() {
		return new ElasticsearchEntityService(client, INDEX_NAME, "project", false);
	}

	@Produces
	@Named("contributorServiceBackend")
	public EntityService produceContributorService() {
		return new ElasticsearchEntityService(client, INDEX_NAME, "contributor", false);
	}

}
