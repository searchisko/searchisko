/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import java.util.Properties;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.searchisko.api.model.AppConfiguration.ClientType;
import org.searchisko.api.util.SearchUtils;

/**
 * Search Client service for Elasticsearch client
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
		log = Logger.getLogger(getClass().getName());
		Properties settings = SearchUtils.loadProperties("/search_client_settings.properties");

		if (ClientType.EMBEDDED.equals(appConfigurationService.getAppConfiguration().getClientType())) {
			node = createEmbeddedNode("search", settings);
			client = node.client();
			return;
		} else {
			Properties transportAddresses = SearchUtils.loadProperties("/search_client_connections.properties");
			client = createTransportClient(transportAddresses, settings);
		}

		checkHealthOfCluster(client);
	}

}
