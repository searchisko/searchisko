/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.searchisko.api.ContentObjectFields;
import org.searchisko.api.cache.RegisteredQueryCache;
import org.searchisko.api.util.SearchUtils;
import org.searchisko.persistence.service.EntityService;
import org.searchisko.persistence.service.ListRequest;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.StreamingOutput;
import java.util.*;

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
	 * Field in registered query definition with description.
	 */
	public static final String FIELD_DESCRIPTION = "description";

	/**
	 * Field in registered query definition with list of roles.
	 */
	public static final String FIELD_ALLOWED_ROLES = "roles";

	/**
	 * Field in registered query definition specifying default sys_type and/or sys_content_type values.
	 */
	public static final String FIELD_DEFAULT = "default";

	/**
	 * Field in registered query definition specifying names of URL parameters
	 * that override the default sys_type and/or sys_content_type values.
	 */
	public static final String FIELD_OVERRIDE = "override";

	/**
	 * Field in registered query definition with the template.
	 */
	public static final String FIELD_TEMPLATE = "template";

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

	@Inject
	protected RegisteredQueryCache registeredQueryCache;

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

	/**
	 * Find registered query based on its id. Values are cached here with timeout so
	 * may provide rather obsolete data sometimes!
	 *
	 * @param id of registered query - system wide unique
	 * @return provider configuration
	 */
	public Map<String, Object> findRegisteredQuery(String id) {
		if (SearchUtils.trimToNull(id) == null)
			return null;
		Map<String, Object> ret = registeredQueryCache.get(id);
		if (ret == null) {
			ret = get(id);
			if (ret != null)
				registeredQueryCache.put(id, ret);
		}
		return ret;
	}

	@Override
	public String create(Map<String, Object> entity) {
		String id = entityService.create(entity);
		updateSearchIndex(id, entity);
		searchClientService.getClient().preparePutIndexedScript()
				.setScriptLang(TEMPLATE_LANGUAGE).setId(id).setSource(entity).execute().actionGet();
		flushCache();
		return id;
	}

	@Override
	public void create(String id, Map<String, Object> entity) {
		entityService.create(id, entity);
		updateSearchIndex(id, entity);
		searchClientService.getClient().preparePutIndexedScript()
				.setScriptLang(TEMPLATE_LANGUAGE).setId(id).setSource(entity).execute().actionGet();
		flushCache();
	}

	@Override
	public void update(String id, Map<String, Object> entity) {
		entityService.update(id, entity);
		updateSearchIndex(id, entity);
		searchClientService.getClient().preparePutIndexedScript()
				.setScriptLang(TEMPLATE_LANGUAGE).setId(id).setSource(entity).execute().actionGet();
		flushCache();
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
		flushCache();
	}

	@Override
	public ListRequest listRequestInit() {
		return entityService.listRequestInit();
	}

	@Override
	public ListRequest listRequestNext(ListRequest previous) {
		return entityService.listRequestNext(previous);
	}

	/**
	 * Flush cache containing data extracted from Registered Query definitions.
	 */
	public void flushCache() {
		if (registeredQueryCache != null)
			registeredQueryCache.flush();
	}

	/**
	 * @param id
	 * @return array of defaults sys_type values
	 */
	public String[] getDefaultSysTypes(String id) {
		return getDefaultValues(id, ContentObjectFields.SYS_TYPE);
	}

	/**
	 * @param id
	 * @return array of defaults sys_content_type values
	 */
	public String[] getDefaultSysContentTypes(String id) {
		return getDefaultValues(id, ContentObjectFields.SYS_CONTENT_TYPE);
	}

	private String[] getDefaultValues(String id, String fieldName) {
		return getConfigSecondLevelValues(id, FIELD_DEFAULT, fieldName, true);
	}

	/**
	 * @param id
	 * @return value of override sys_type or null
	 */
	public String getOverrideSysTypes(String id) {
		String[] values = getOverrideValues(id, ContentObjectFields.SYS_TYPE);
		return values.length > 0 ? values[0] : null;
	}

	/**
	 * @param id
	 * @return value of override sys_content_type or null
	 */
	public String getOverrideSysContentTypes(String id) {
		String[] values = getOverrideValues(id, ContentObjectFields.SYS_CONTENT_TYPE);
		return values.length > 0 ? values[0] : null;
	}

	private String[] getOverrideValues(String id, String fieldName) {
		return getConfigSecondLevelValues(id, FIELD_OVERRIDE, fieldName, false);
	}

	@SuppressWarnings("unchecked")
	protected String[] getConfigSecondLevelValues(String id, String topLevelName, String fieldName, boolean multiValuesAllowed) {
		List<String> ret = new ArrayList<>();
		Map<String, Object> config = findRegisteredQuery(id);
		if (config != null) {
			Object section = config.get(topLevelName);
			if (section != null && section instanceof Map) {
				try {
					Object v = ((Map) section).get(fieldName);
					if (v instanceof String) {
						ret.add((String) v);
					} else if (v instanceof String[] && multiValuesAllowed) {
						Collections.addAll(ret, (String[]) v);
					} else if (v instanceof Collection && multiValuesAllowed) {
						ret.addAll((Collection<? extends String>) v);
					}
				} catch (ClassCastException | NullPointerException e) {
					// cast errors or NPE, we can ignore... probably invalid configuration
				}
			}
		}
		return ret.toArray(new String[ret.size()]);
	}
}
