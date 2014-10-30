/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.searchisko.persistence.service.EntityService;
import org.searchisko.persistence.service.ListRequest;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.StreamingOutput;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Service related to Registered Queries definitions.
 *
 * @author Lukas Vlcek
 */
@Named
@Stateless
@LocalBean
public class RegisteredQueryService implements SearchableEntityService {

	private static final String TEMPLATE_LANGUAGE = "mustache";

	/**
	 * Field in registered query definition with query id.
	 */
	public static final String FIELD_NAME = "id";

	/**
	 * Field in registered query definition with the template.
	 */
	public static final String FIELD_TEMPLATE = "template";

	/**
	 * Field in registered query definition with list of roles.
	 */
	public static final String FIELD_ALLOWED_ROLES = "roles";

	/**
	 * Field in registered query definition with description.
	 */
	public static final String FIELD_DESCRIPTION = "description";

	/**
	 * Name of ES search index where queries are stored.
	 */
	public static final String SEARCH_INDEX_NAME = "sys_queries";
	/**
	 * Name of ES search index type under which queries are stored.
	 */
	public static final String SEARCH_INDEX_TYPE = "query";

	@Inject
	protected SearchClientService searchClientService;

	@Inject
	@Named("queryServiceBackend")
	protected EntityService entityService;

	protected void updateSearchIndex(String id, Map<String, Object> entity) {
		searchClientService.performPut(SEARCH_INDEX_NAME, SEARCH_INDEX_TYPE, id, entity);
	}

	@Override
	public BulkRequestBuilder prepareBulkRequest() {
		return searchClientService.getClient().prepareBulk();
	}

	@Override
	public void updateSearchIndex(BulkRequestBuilder brb, String id, Map<String, Object> entity) {
		brb.add(searchClientService.getClient().prepareIndex(SEARCH_INDEX_NAME, SEARCH_INDEX_TYPE, id).setSource(entity));
	}

	@Override
	public void deleteOldFromSearchIndex(Date timestamp) {
		searchClientService.performDeleteOldRecords(SEARCH_INDEX_NAME, SEARCH_INDEX_TYPE, timestamp);
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

	@Override
	public String create(Map<String, Object> entity) {
		String id = entityService.create(entity);
		updateSearchIndex(id, entity);
		searchClientService.getClient().preparePutIndexedScript()
				.setScriptLang(TEMPLATE_LANGUAGE).setId(id).setSource(entity).execute().actionGet();
		return id;
	}

	@Override
	public void create(String id, Map<String, Object> entity) {
		entityService.create(id, entity);
		updateSearchIndex(id, entity);
		searchClientService.getClient().preparePutIndexedScript()
				.setScriptLang(TEMPLATE_LANGUAGE).setId(id).setSource(entity).execute().actionGet();
	}

	@Override
	public void update(String id, Map<String, Object> entity) {
		entityService.update(id, entity);
		updateSearchIndex(id, entity);
		searchClientService.getClient().preparePutIndexedScript()
				.setScriptLang(TEMPLATE_LANGUAGE).setId(id).setSource(entity).execute().actionGet();
	}

	/**
	 * Delete registered query.
	 *
	 * Internally it first delete persisted record (via entity service) and then it tries to delete
	 * registered query from sys_* index (ignores exception) and then it delete indexed script
	 * via ES client (delete search template stored in ES).
	 *
	 * @param id of entity to delete
	 */
	@Override
	public void delete(String id) {
		entityService.delete(id);
		try {
			searchClientService.performDelete(SEARCH_INDEX_NAME, SEARCH_INDEX_TYPE, id);
		} catch (SearchIndexMissingException e) {
			// OK
		}
		searchClientService.getClient().prepareDeleteIndexedScript(TEMPLATE_LANGUAGE, id).execute().actionGet();
	}

	@Override
	public ListRequest listRequestInit() {
		return entityService.listRequestInit();
	}

	@Override
	public ListRequest listRequestNext(ListRequest previous) {
		return entityService.listRequestNext(previous);
	}
}
