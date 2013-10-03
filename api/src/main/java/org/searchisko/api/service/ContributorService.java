/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.StreamingOutput;

import org.elasticsearch.action.admin.indices.flush.FlushRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.indices.IndexMissingException;
import org.searchisko.api.util.Resources;
import org.searchisko.persistence.service.EntityService;

/**
 * Service related to Contributor
 *
 * @author Libor Krzyzanek
 * @author Vlastimil Elias (velias at redhat dot com)
 *
 */
@Named
@Stateless
@LocalBean
public class ContributorService implements EntityService {

	@Inject
	protected Logger log;

	public static final String SEARCH_INDEX_NAME = "sys_contributors";

	public static final String SEARCH_INDEX_TYPE = "contributor";

	@Inject
	protected SearchClientService searchClientService;

	@Inject
	@Named("contributorServiceBackend")
	protected EntityService entityService;

	@PostConstruct
	public void init() {
		try {
			Client client = searchClientService.getClient();
			if (!client.admin().indices().prepareExists(SEARCH_INDEX_NAME).execute().actionGet().isExists()) {
				log.info("Contributor search index called '" + SEARCH_INDEX_NAME
						+ "' doesn't exists. Creating it together with mapping for type '" + SEARCH_INDEX_TYPE + "'");
				client.admin().indices().prepareCreate(SEARCH_INDEX_NAME).execute().actionGet();
				client.admin().indices().preparePutMapping(SEARCH_INDEX_NAME).setType(SEARCH_INDEX_TYPE)
						.setSource(Resources.readStringFromClasspathFile("/mappings/contributor.json")).execute().actionGet();
			} else {
				log.info("Contributor search index called '" + SEARCH_INDEX_NAME + "' exists already.");
			}
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	@Override
	public StreamingOutput getAll(Integer from, Integer size, String[] fieldsToRemove) {
		return entityService.getAll(from, size, fieldsToRemove);
	}

	@Override
	public List<Map<String, Object>> getAll() {
		return entityService.getAll();
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
		searchClientService.getClient().prepareIndex(SEARCH_INDEX_NAME, SEARCH_INDEX_TYPE, id).setSource(entity).execute()
				.actionGet();
		searchClientService.getClient().admin().indices().flush(new FlushRequest(SEARCH_INDEX_NAME));
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

	public SearchResponse search(String email) {
//		try {
			SearchRequestBuilder searchBuilder = searchClientService.getClient().prepareSearch(SEARCH_INDEX_NAME)
					.setTypes(SEARCH_INDEX_TYPE);

			searchBuilder.setFilter(FilterBuilders.queryFilter(QueryBuilders.matchQuery("email", email)));
			searchBuilder.setQuery(QueryBuilders.matchAllQuery());

			try {
				final SearchResponse response = searchBuilder.execute().actionGet();
				return response;
			} catch (IndexMissingException e) {
				return null;
			}
//		} catch (Exception e) {
//			throw e;
//		}
	}
}
