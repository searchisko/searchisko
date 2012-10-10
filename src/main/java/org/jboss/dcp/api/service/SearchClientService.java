/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.service;

import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.elasticsearch.client.Client;
import org.jboss.dcp.api.model.AppConfiguration.ClientType;
import org.jboss.dcp.api.util.SearchUtils;

/**
 * Search Client service for ES client
 * 
 * @author Libor Krzyzanek
 * 
 */
@Named
@ApplicationScoped
@Singleton
@Startup
public class SearchClientService extends ElasticsearchClientService {

	@PostConstruct
	public void init() throws Exception {
		Properties settings = SearchUtils.loadProperties("/search_client_settings.properties");

		if (ClientType.EMBEDDED.equals(appConfigurationService.getAppConfiguration().getClientType())) {
			node = createEmbeddedNode("search", settings);
			client = node.client();

			if (!client.admin().indices().prepareExists(ContributorService.SEARCH_INDEX_NAME).execute().actionGet()
					.exists()) {
				client.admin().indices().prepareCreate(ContributorService.SEARCH_INDEX_NAME).execute().actionGet();
				// TODO: Set proper mapping for Contributor index
				// client.admin().indices().preparePutMapping(ContributorService.SEARCH_INDEX_NAME)
				// .setType(ContributorService.SEARCH_INDEX_TYPE).setSource("").execute().actionGet();
			}

			return;
		} else {
			Properties transportAddresses = SearchUtils.loadProperties("/search_client_connections.properties");
			client = createTransportClient(transportAddresses, settings);
		}

		checkHealthOfCluster(client);
	}

	public Client getClient() {
		return client;
	}

}
