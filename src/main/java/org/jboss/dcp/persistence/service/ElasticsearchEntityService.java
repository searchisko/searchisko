/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.persistence.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.StreamingOutput;

import org.elasticsearch.action.admin.indices.flush.FlushRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.SearchHit;
import org.jboss.dcp.api.rest.ESDataOnlyResponse;

/**
 * Implementation of entity service with elastic search as back-end.<br/>
 * Entity name is index type in elastic search index
 * 
 * @author Libor Krzyzanek
 * 
 */
public class ElasticsearchEntityService implements EntityService {

	protected Client client;

	protected String indexName;

	/**
	 * Same as entity name
	 */
	protected String indexType;

	// TODO: Remove OperationThreaded and use refresh if it will be needed.
	protected boolean operationThreaded;

	public ElasticsearchEntityService(Client client, String indexName, String entityName, boolean operationThreaded) {
		this.client = client;
		this.indexName = indexName;
		this.indexType = entityName;
		this.operationThreaded = operationThreaded;
	}

	public Client getClient() {
		return client;
	}

	/**
	 * Utility to create {@link SearchRequestBuilder} with predefined indices and type
	 * 
	 * @return
	 */
	public SearchRequestBuilder createSearchRequestBuilder() {
		return client.prepareSearch(indexName).setTypes(indexType);
	}

	/**
	 * Perform search
	 * 
	 * @param searchBuilder
	 * @return
	 */
	public SearchResponse search(SearchRequestBuilder searchBuilder) {
		return searchBuilder.execute().actionGet();
	}

	@Override
	public StreamingOutput getAll(Integer from, Integer size, String[] fieldsToRemove) {
		SearchRequestBuilder srb = new SearchRequestBuilder(client);
		srb.setQuery(QueryBuilders.matchAllQuery());
		srb.setIndices(indexName);
		srb.setTypes(indexType);

		if (from != null) {
			srb.setFrom(from);
		}
		if (size != null) {
			srb.setSize(size);
		}

		try {
			final SearchResponse response = srb.execute().actionGet();
			return new ESDataOnlyResponse(response, fieldsToRemove);
		} catch (IndexMissingException e) {
			return new ESDataOnlyResponse(null);
		}

	}

	@Override
	public List<Map<String, Object>> getAll() {
		SearchRequestBuilder srb = new SearchRequestBuilder(client);
		srb.setQuery(QueryBuilders.matchAllQuery());
		srb.setIndices(indexName);
		srb.setTypes(indexType);
		srb.setSize(1000000);

		List<Map<String, Object>> ret = new ArrayList<Map<String, Object>>();
		try {
			final SearchResponse response = srb.execute().actionGet();
			if (response.getHits().getTotalHits() > 0) {
				for (SearchHit hit : response.getHits().getHits()) {
					ret.add(hit.getSource());
				}
			}
			return ret;
		} catch (IndexMissingException e) {
			return ret;
		}
	}

	@Override
	public Map<String, Object> get(String id) {
		try {
			return client.prepareGet(indexName, indexType, id).setOperationThreaded(operationThreaded).execute().actionGet()
					.getSource();
		} catch (IndexMissingException e) {
			return null;
		}
	}

	@Override
	public String create(Map<String, Object> entity) {
		IndexResponse response = client.prepareIndex(indexName, indexType).setOperationThreaded(operationThreaded)
				.setSource(entity).execute().actionGet();
		client.admin().indices().flush(new FlushRequest(indexName));
		return response.getId();
	}

	@Override
	public void create(String id, Map<String, Object> entity) {
		update(id, entity);
	}

	@Override
	public void update(String id, Map<String, Object> entity) {
		client.prepareIndex(indexName, indexType, id).setOperationThreaded(operationThreaded).setSource(entity).execute()
				.actionGet();
		client.admin().indices().flush(new FlushRequest(indexName));
	}

	@Override
	public void delete(String id) {
		client.prepareDelete(indexName, indexType, id).setOperationThreaded(operationThreaded).execute().actionGet();
		client.admin().indices().flush(new FlushRequest(indexName));
	}
}
