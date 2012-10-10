/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.service;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;

/**
 * Service related to Contributor
 * 
 * @author Libor Krzyzanek
 * 
 */
@Named
@ApplicationScoped
public class ContributorService implements EntityService {

	public static final String SEARCH_INDEX_NAME = "dcp_contributors";

	public static final String SEARCH_INDEX_TYPE = "contributor";

	@Inject
	private SearchClientService searchClientService;

	@Inject
	@Named("contributorServiceBackend")
	private EntityService entityService;

	@Override
	public Object getAll(Integer from, Integer size) {
		return entityService.getAll(from, size);
	}

	@Override
	public Map<String, Object> get(String id) {
		return entityService.get(id);
	}

	/**
	 * Updates search index by current entity identified by id
	 * 
	 * @param id
	 * @param entity
	 */
	private void updateSearchIndex(String id, Map<String, Object> entity) {
		searchClientService.getClient().prepareIndex(SEARCH_INDEX_NAME, SEARCH_INDEX_TYPE, id).setSource(entity)
				.execute().actionGet();
	}

	@Override
	public String create(Map<String, Object> entity) {
		String id = entityService.create(entity);

		updateSearchIndex(id, entity);

		return id;
	}

	@Override
	public void create(String id, Map<String, Object> entity) {
		entityService.create(id, entity);
		updateSearchIndex(id, entity);
	}

	@Override
	public void update(String id, Map<String, Object> entity) {
		entityService.update(id, entity);
		updateSearchIndex(id, entity);
	}

	@Override
	public void delete(String id) {
		entityService.delete(id);
		searchClientService.getClient().prepareDelete(SEARCH_INDEX_NAME, SEARCH_INDEX_TYPE, id).execute().actionGet();
	}

	public SearchResponse search(String email) throws Exception {
		try {
			SearchRequestBuilder searchBuilder = searchClientService.getClient().prepareSearch(SEARCH_INDEX_NAME)
					.setTypes(SEARCH_INDEX_TYPE);

			searchBuilder.setFilter(FilterBuilders.queryFilter(QueryBuilders.textQuery("email", email)));
			searchBuilder.setQuery(QueryBuilders.matchAllQuery());

			final SearchResponse response = searchBuilder.execute().actionGet();

			return response;
		} catch (Exception e) {
			throw e;
		}
	}
}
