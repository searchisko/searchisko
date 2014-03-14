/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.action.admin.indices.flush.FlushRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.indices.IndexMissingException;
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
	 * Perform filter query into one ES index and type with one field and value.
	 * 
	 * @param indexName to search in
	 * @param indexType to search
	 * @param fieldName name of field to search
	 * @param fieldValue value to search for
	 * @return
	 */
	public SearchResponse performFilterByOneField(String indexName, String indexType, String fieldName, String fieldValue)
			throws SearchIndexMissingException {
		try {
			SearchRequestBuilder searchBuilder = getClient().prepareSearch(indexName).setTypes(indexType);

			searchBuilder.setFilter(FilterBuilders.queryFilter(QueryBuilders.matchQuery(fieldName, fieldValue)));
			searchBuilder.setQuery(QueryBuilders.matchAllQuery());

			final SearchResponse response = searchBuilder.execute().actionGet();
			return response;
		} catch (IndexMissingException e) {
			log.log(Level.WARNING, e.getMessage());
			throw new SearchIndexMissingException(e);
		}
	}

	/**
	 * Perform filter query into one ES index and type to get records with any value stored in one field.
	 * 
	 * @param indexName to search in
	 * @param indexType to search
	 * @param fieldName name of field to search for any value in it
	 * @return
	 */
	public SearchResponse performQueryByOneFieldAnyValue(String indexName, String indexType, String fieldName)
			throws SearchIndexMissingException {
		try {
			SearchRequestBuilder searchBuilder = getClient().prepareSearch(indexName).setTypes(indexType);

			searchBuilder.setFilter(FilterBuilders.notFilter(FilterBuilders.missingFilter(fieldName)));
			searchBuilder.setQuery(QueryBuilders.matchAllQuery());

			final SearchResponse response = searchBuilder.execute().actionGet();
			return response;
		} catch (IndexMissingException e) {
			log.log(Level.WARNING, e.getMessage());
			throw new SearchIndexMissingException(e);
		}
	}

	/**
	 * Perform ElasticSearch document GET request.
	 * 
	 * @param indexName name of ES index to get document from
	 * @param indexType type of ES document to get
	 * @param id of document to get
	 * @return ES get response
	 * @throws SearchIndexMissingException
	 * @throws ElasticSearchException if something is wrong
	 */
	public GetResponse performGet(String indexName, String indexType, String id) throws SearchIndexMissingException {
		try {
			return getClient().prepareGet(indexName, indexType, id).execute().actionGet();
		} catch (IndexMissingException e) {
			throw new SearchIndexMissingException(e);
		}
	}

	/**
	 * Perform asynchronous ElasticSearch document PUT (doc into index) request - no wait to response.
	 * 
	 * @param indexName ES index name to put document into
	 * @param indexType type of ES document to put
	 * @param id of ES document to put
	 * @param content of document to put
	 */
	public void performPutAsync(String indexName, String indexType, String id, Map<String, Object> content) {
		getClient().prepareIndex(indexName, indexType, id).setSource(content).execute();
	}

	/**
	 * Perform synchronous ElasticSearch document PUT (doc into index) request - wait to response and return it.<br/>
	 * ID is generated by ElasticSearch
	 * 
	 * @param indexName ES index name to put document into
	 * @param indexType type of ES document to put
	 * @param content of document to put
	 * @return ES response object
	 * @throws ElasticSearchException if something is wrong
	 */
	public IndexResponse performPut(String indexName, String indexType, Map<String, Object> content) {
		return getClient().prepareIndex(indexName, indexType).setSource(content).execute().actionGet();
	}

	/**
	 * Perform synchronous ElasticSearch document PUT (doc into index) request - wait to response and return it.
	 * 
	 * @param indexName ES index name to put document into
	 * @param indexType type of ES document to put
	 * @param id of ES document to put
	 * @param content of document to put
	 * @return ES response object
	 * @throws ElasticSearchException if something is wrong
	 */
	public IndexResponse performPut(String indexName, String indexType, String id, Map<String, Object> content) {
		return getClient().prepareIndex(indexName, indexType, id).setSource(content).execute().actionGet();
	}

	/**
	 * Delete document from ES index
	 * 
	 * @param indexName to delete from
	 * @param indexType to delete
	 * @param id of document to delete
	 * @return {@link DeleteResponse}
	 * @throws SearchIndexMissingException
	 * @throws ElasticSearchException if something is wrong
	 */
	public DeleteResponse performDelete(String indexName, String indexType, String id) throws SearchIndexMissingException {
		try {
			return getClient().prepareDelete(indexName, indexType, id).execute().actionGet();
		} catch (IndexMissingException e) {
			throw new SearchIndexMissingException(e);
		}
	}

	/**
	 * Perform ES index flush and refresh operations.
	 * 
	 * @param indexName to flush
	 */
	public void performIndexFlushAndRefresh(String... indexName) {
		try {
			getClient().admin().indices().flush(new FlushRequest(indexName));
			getClient().admin().indices().refresh(new RefreshRequest(indexName));
		} catch (IndexMissingException e) {
			// OK
		}
	}

	/**
	 * Perform ES index flush and refresh operations - block current thread until operation is performed.
	 * 
	 * @param indexName to flush
	 */
	public void performIndexFlushAndRefreshBlocking(String... indexName) {
		try {
			getClient().admin().indices().flush(new FlushRequest(indexName)).actionGet();
			getClient().admin().indices().refresh(new RefreshRequest(indexName)).actionGet();
		} catch (IndexMissingException e) {
			// OK
		}
	}

}
