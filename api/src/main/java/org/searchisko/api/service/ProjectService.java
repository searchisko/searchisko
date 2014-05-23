/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.StreamingOutput;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.searchisko.persistence.service.EntityService;
import org.searchisko.persistence.service.ListRequest;

/**
 * Service related to Project definitions.
 * 
 * @author Libor Krzyzanek
 * @author Lukas Vlcek
 * @author Vlastimil Elias (velias at redhat dot com)
 */
@Named
@Stateless
@LocalBean
public class ProjectService implements SearchableEntityService {

	/**
	 * Field in project definition with project code, used as unique ID
	 */
	public static final String FIELD_CODE = "code";

	/**
	 * Field in project definition with project name
	 */
	public static final String FIELD_NAME = "name";

	/**
	 * Field in project definition containing Map structure with other unique identifiers used to map pushed data to the
	 * project. Key in the Map structure marks type of identifier (eg. jbossorg_jira, jbossorg_project_info), value in
	 * structure is identifier or array of identifiers itself used during mapping.
	 * 
	 * @see {@link #findByTypeSpecificCode(String, String)} for description.
	 */
	public static final String FIELD_TYPE_SPECIFIC_CODE = "type_specific_code";

	/**
	 * Name of ES search index where projects are stored.
	 */
	public static final String SEARCH_INDEX_NAME = "sys_projects";
	/**
	 * Name of ES search index type under which projects are stored.
	 */
	public static final String SEARCH_INDEX_TYPE = "project";

	@Inject
	protected Logger log;

	@Inject
	protected SearchClientService searchClientService;

	@Inject
	@Named("projectServiceBackend")
	protected EntityService entityService;

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

	protected void updateSearchIndex(String id, Map<String, Object> entity) {
		searchClientService.performPut(SEARCH_INDEX_NAME, SEARCH_INDEX_TYPE, id, entity);
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
		try {
			searchClientService.performDelete(SEARCH_INDEX_NAME, SEARCH_INDEX_TYPE, id);
		} catch (SearchIndexMissingException e) {
			// OK
		}
	}

	/**
	 * Find project by <code>code</code> (unique id used in content).
	 * 
	 * @param code to search project for.
	 * 
	 * @return search result - should contain zero or one project only! Multiple projects for one code is configuration
	 *         problem!
	 */
	public SearchResponse findByCode(String code) {
		try {
			return searchClientService.performFilterByOneField(SEARCH_INDEX_NAME, SEARCH_INDEX_TYPE, FIELD_CODE, code);
		} catch (SearchIndexMissingException e) {
			return null;
		}
	}

	/**
	 * Find project by 'type specific code'. These codes are used to map from third party unique identifiers to searchisko
	 * unique project id.
	 * 
	 * @param codeName name of 'type specific code', eg. <code>jbossorg_jira</code>, <code>jbossorg_project_info</code>
	 * @param codeValue value of code to search for
	 * @return search result - should contain zero or one project only! Multiple projects for one code is configuration
	 *         problem!
	 */
	public SearchResponse findByTypeSpecificCode(String codeName, String codeValue) {
		try {
			return searchClientService.performFilterByOneField(SEARCH_INDEX_NAME, SEARCH_INDEX_TYPE, FIELD_TYPE_SPECIFIC_CODE
					+ "." + codeName, codeValue);
		} catch (SearchIndexMissingException e) {
			return null;
		}
	}

	@Override
	public ListRequest listRequestInit() {
		return entityService.listRequestInit();
	}

	@Override
	public ListRequest listRequestNext(ListRequest previous) {
		return entityService.listRequestNext(previous);
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

}
