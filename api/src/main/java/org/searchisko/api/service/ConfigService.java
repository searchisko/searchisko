/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.StreamingOutput;

import org.searchisko.persistence.service.EntityService;
import org.searchisko.persistence.service.ListRequest;

/**
 * Service related to config documents store handling
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 * @author Libor Krzyzanek
 * @author Lukas Vlcek
 */
@Named
@Stateless
@LocalBean
public class ConfigService implements EntityService {

	public static final String CFGNAME_SEARCH_RESPONSE_FIELDS = "search_response_fields";
	public static final String CFGNAME_SEARCH_FULLTEXT_QUERY_FIELDS = "search_fulltext_query_fields";
	public static final String CFGNAME_SEARCH_FULLTEXT_HIGHLIGHT_FIELDS = "search_fulltext_highlight_fields";
	public static final String CFGNAME_SEARCH_FULLTEXT_AGGREGATIONS_FIELDS = "search_fulltext_aggregations_fields";
	public static final String CFGNAME_SEARCH_FULLTEXT_FILTER_FIELDS = "search_fulltext_filter_fields";
	public static final String CFGNAME_NORMALIZATIONS = "normalizations";
	public static final String CFGNAME_SECURITY_RESTAPI_CORS = "security_restapi_cors";

	@Inject
	protected Logger log;

	@Inject
	@Named("configServiceBackend")
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

	@Override
	public String create(Map<String, Object> entity) {
		String id = entityService.create(entity);
		return id;
	}

	@Override
	public void create(String id, Map<String, Object> entity) {
		entityService.create(id, entity);
	}

	@Override
	public void update(String id, Map<String, Object> entity) {
		entityService.update(id, entity);
	}

	@Override
	public void delete(String id) {
		entityService.delete(id);
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
