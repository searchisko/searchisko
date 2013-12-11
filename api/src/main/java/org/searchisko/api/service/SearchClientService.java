/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.elasticsearch.action.get.GetResponse;
import org.searchisko.api.model.AppConfiguration.ClientType;
import org.searchisko.api.util.SearchUtils;

/**
 * Search Client service for Elasticsearch client
 * 
 * @author Libor Krzyzanek
 * @author Vlastimil Elias (velias at redhat dot com)
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

	/**
	 * Perform ElasticSearch document GET request.
	 * 
	 * @param indexName name of ES index to get document from
	 * @param indexType type of ES document to get
	 * @param id of document to get
	 * @return ES get response
	 */
	public GetResponse performGet(String indexName, String indexType, String id) {
		return getClient().prepareGet(indexName, indexType, id).execute().actionGet();
	}

	/**
	 * Perform asynchronous ElasticSearch document PUT (doc into index) request - no waut to response.
	 * 
	 * @param indexName ES index name to put document into
	 * @param indexType type of ES document to put
	 * @param id of ES document to put
	 * @param content of document to put
	 */
	public void performPutAsync(String indexName, String indexType, String id, Map<String, Object> content) {
		getClient().prepareIndex(indexName, indexType, id).setSource(content).execute();
	}

}
