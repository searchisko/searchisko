/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.rest;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Requests;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.jboss.dcp.api.annotations.security.GuestAllowed;
import org.jboss.dcp.api.model.QuerySettings;
import org.jboss.dcp.api.query.ProxyQueryBuilder;
import org.jboss.dcp.api.service.StatsRecordType;

/**
 * Search REST API
 * 
 * @author Libor Krzyzanek
 * 
 */
@RequestScoped
@Path("/search")
@Produces(MediaType.APPLICATION_JSON)
public class SearchRestService extends RestServiceBase {

	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	@GuestAllowed
	public Object search(@Context UriInfo uriInfo) {

		QuerySettings settings = null;
		try {
			// TODO: Rewrite parsing settings from request to Jax-RS facilities
			// MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
			// settings = QuerySettingsParser.parseSettings();
			settings = new QuerySettings();
		} catch (Exception e) {
			return createErrorResponse(e);
		}

		try {
			SearchSourceBuilder sb = null;
			sb = ProxyQueryBuilder.buildSearchQuery(settings);
			sb.timeout(TimeValue.timeValueSeconds(getTimeout().search()));

			// TODO send request only to selected projects instead to _all
			SearchRequest sr = Requests.searchRequest("_all");

			if (settings.getCount()) {
				sr.searchType(SearchType.COUNT);
			} else {
				sr.searchType(SearchType.DFS_QUERY_THEN_FETCH);
			}
			sr.source(sb);

			SearchResponse searchResponse = getSearchClientService().getClient().search(sr).actionGet();

			return createResponse(searchResponse);
		} catch (ElasticSearchException e) {
			getStatsClientService().writeStatistics(StatsRecordType.SEARCH, e, System.currentTimeMillis(),
					settings.getQuery(), settings.getFilters());
			return createErrorResponse(e);
		} catch (Exception e) {
			return createErrorResponse(e);
		}

	}
}
