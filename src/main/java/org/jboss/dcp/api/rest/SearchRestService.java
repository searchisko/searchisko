/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.rest;

import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.jboss.dcp.api.annotations.security.GuestAllowed;
import org.jboss.dcp.api.model.QuerySettings;
import org.jboss.dcp.api.model.QuerySettings.SortByValue;
import org.jboss.dcp.api.service.ProviderService;
import org.jboss.dcp.api.service.StatsRecordType;
import org.jboss.dcp.api.util.QuerySettingsParser;

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

	@Inject
	private ProviderService providerService;

	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	@GuestAllowed
	public Object search(@Context UriInfo uriInfo) {

		QuerySettings settings = null;
		try {
			MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
			settings = QuerySettingsParser.parseUriParams(params);
		} catch (Exception e) {
			return createErrorResponse(e);
		}

		try {
			SearchRequestBuilder srb = new SearchRequestBuilder(getSearchClientService().getClient());
			if (settings.getContentType() != null) {
				Map<String, Object> typeDef = providerService.findContentType(settings.getContentType());
				if (typeDef == null) {
					return createBadFieldDataResponse("type");
				}

				String indexName = ProviderService.getIndexName(typeDef);
				String indexType = ProviderService.getIndexType(typeDef);

				srb.setIndices(indexName);
				srb.setTypes(indexType);
			} else {
				srb.setIndices("_all");
			}

			srb.setTimeout(TimeValue.timeValueSeconds(getTimeout().search()));

			if (settings.getQuery() != null) {
				srb.setQuery(QueryBuilders.queryString(settings.getQuery()));
			} else {
				srb.setQuery(QueryBuilders.matchAllQuery());
			}

			if (settings.getFilters().getStart() != null) {
				srb.setFrom(settings.getFilters().getStart());
			}

			if (settings.getFilters().getCount() != null) {
				srb.setSize(settings.getFilters().getCount());
			}

			if (settings.getSortBy() != null) {
				if (settings.getSortBy().compareTo(SortByValue.NEW) == 0) {
					srb.addSort("dcp_updated", SortOrder.ASC);
				} else if (settings.getSortBy().compareTo(SortByValue.OLD) == 0) {
					srb.addSort("dcp_updated", SortOrder.DESC);
				}
			}

			final SearchResponse searchResponse = srb.execute().actionGet();

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
