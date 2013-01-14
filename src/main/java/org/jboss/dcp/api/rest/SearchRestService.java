/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.settings.SettingsException;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.AndFilterBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilteredQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermsFilterBuilder;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.sort.SortOrder;
import org.jboss.dcp.api.annotations.security.GuestAllowed;
import org.jboss.dcp.api.model.QuerySettings;
import org.jboss.dcp.api.model.QuerySettings.SortByValue;
import org.jboss.dcp.api.service.ProviderService;
import org.jboss.dcp.api.service.StatsRecordType;
import org.jboss.dcp.api.util.QuerySettingsParser;

/**
 * Search REST API.
 * 
 * @author Libor Krzyzanek
 * @author Vlastimil Elias (velias at redhat dot com)
 * 
 */
@RequestScoped
@Path("/search")
@Produces(MediaType.APPLICATION_JSON)
public class SearchRestService extends RestServiceBase {

	@Inject
	protected ProviderService providerService;

	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	@GuestAllowed
	public Object search(@Context UriInfo uriInfo) {

		QuerySettings querySettings = null;
		try {
			MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
			querySettings = QuerySettingsParser.parseUriParams(params);

			SearchRequestBuilder srb = new SearchRequestBuilder(getSearchClientService().getClient());

			handleSearchInicesAndTypes(querySettings, srb);

			QueryBuilder qb = handleFulltextSearchSettings(querySettings);
			qb = handleCommonFiltersSettings(querySettings, qb);
			srb.setQuery(qb);

			handleSortingSettings(querySettings, srb);

			// TODO _SEARCH return facets data depending on 'facet' params

			handleResponseContentSettings(querySettings, srb);
			srb.setTimeout(TimeValue.timeValueSeconds(getTimeout().search()));

			log.log(Level.FINE, "ElasticSearch Search request: {0}", srb);

			final SearchResponse searchResponse = srb.execute().actionGet();

			getStatsClientService().writeStatistics(StatsRecordType.SEARCH, searchResponse, System.currentTimeMillis(),
					querySettings.getQuery(), querySettings.getFilters());

			addSimpleCORSSourceResponseHeader();
			return createResponse(searchResponse);
		} catch (IllegalArgumentException e) {
			return createBadFieldDataResponse(e.getMessage());
		} catch (IndexMissingException e) {
			return Response.status(Response.Status.NOT_FOUND).build();
		} catch (ElasticSearchException e) {
			getStatsClientService().writeStatistics(StatsRecordType.SEARCH, e, System.currentTimeMillis(),
					querySettings.getQuery(), querySettings.getFilters());
			return createErrorResponse(e);
		} catch (Exception e) {
			return createErrorResponse(e);
		}
	}

	/**
	 * @param querySettings
	 * @param srb
	 */
	protected void handleSearchInicesAndTypes(QuerySettings querySettings, SearchRequestBuilder srb) {
		if (querySettings.getFilters() != null && querySettings.getFilters().getContentType() != null) {
			String type = querySettings.getFilters().getContentType();
			Map<String, Object> typeDef = providerService.findContentType(type);
			if (typeDef == null) {
				throw new IllegalArgumentException("type");
			}
			srb.setIndices(ProviderService.extractSearchIndices(typeDef, type));
			srb.setTypes(ProviderService.extractIndexType(typeDef, type));
		} else {
			String dcpType = null;
			if (querySettings.getFilters() != null) {
				dcpType = querySettings.getFilters().getDcpType();
			}
			Set<String> indexNames = new LinkedHashSet<String>();
			List<Map<String, Object>> allProviders = providerService.listAllProviders();
			for (Map<String, Object> providerCfg : allProviders) {
				try {
					@SuppressWarnings("unchecked")
					Map<String, Map<String, Object>> types = (Map<String, Map<String, Object>>) providerCfg
							.get(ProviderService.TYPE);
					if (types != null) {
						for (String typeName : types.keySet()) {
							Map<String, Object> typeDef = types.get(typeName);
							if ((dcpType == null && !ProviderService.extractSearchAllExcluded(typeDef))
									|| (dcpType != null && dcpType.equals(ProviderService.extractDcpType(typeDef, typeName)))) {
								indexNames.addAll(Arrays.asList(ProviderService.extractSearchIndices(typeDef, typeName)));
							}
						}
					}
				} catch (ClassCastException e) {
					throw new SettingsException("Incorrect configuration of 'type' section for dcp_provider="
							+ providerCfg.get(ProviderService.NAME) + ". Contact administrators please.");
				}
			}

			// TODO _SEARCH extracted index names should be cached with timeout! dcpType must be used for caching!
			srb.setIndices(indexNames.toArray(new String[indexNames.size()]));
		}
	}

	/**
	 * @param querySettings
	 * @return builder for query, newer null
	 */
	protected QueryBuilder handleFulltextSearchSettings(QuerySettings querySettings) {
		if (querySettings.getQuery() != null) {
			// TODO _SEARCH load fields used for fulltext query from DCP configuration changeable during runtime.
			QueryBuilder qb = QueryBuilders.queryString(querySettings.getQuery());
			// TODO _SEARCH perform highlights if param query_highlight=true. Load fields used for highlighting from DCP
			// configuration changeable on runtime
			return qb;
		} else {
			return QueryBuilders.matchAllQuery();
		}

	}

	/**
	 * @param querySettings
	 * @return filter builder if some filters are here, null if not filters necessary
	 */
	protected QueryBuilder handleCommonFiltersSettings(QuerySettings querySettings, QueryBuilder qb) {
		QuerySettings.Filters filters = querySettings.getFilters();
		List<FilterBuilder> searchFilters = new ArrayList<FilterBuilder>();

		// TODO _SEARCH other filtering by: dcp_contributors, activity_date_interval, dcp_activity_dates from,
		// dcp_activity_dates to

		if (filters != null) {
			if (filters.getDcpType() != null) {
				searchFilters.add(new TermsFilterBuilder("dcp_type", filters.getDcpType()));
			}
			if (filters.getDcpContentProvider() != null) {
				searchFilters.add(new TermsFilterBuilder("dcp_content_provider", filters.getDcpContentProvider()));
			}
			if (filters.getTags() != null && !filters.getTags().isEmpty()) {
				searchFilters.add(new TermsFilterBuilder("dcp_tags", filters.getTags()));
			}
			if (filters.getProjects() != null && !filters.getProjects().isEmpty()) {
				searchFilters.add(new TermsFilterBuilder("dcp_project", filters.getProjects()));
			}
		}

		if (!searchFilters.isEmpty()) {
			return new FilteredQueryBuilder(qb, new AndFilterBuilder(searchFilters.toArray(new FilterBuilder[searchFilters
					.size()])));
		} else {
			return qb;
		}
	}

	/**
	 * @param querySettings
	 * @param srb request builder to set sorting for
	 */
	protected void handleSortingSettings(QuerySettings querySettings, SearchRequestBuilder srb) {
		if (querySettings.getSortBy() != null) {
			// TODO _SEARCH ordering by date should be over new field 'dcp_last_activity_date' which should be maximum from
			// dcp_activity_dates
			if (querySettings.getSortBy().equals(SortByValue.NEW)) {
				srb.addSort("dcp_updated", SortOrder.DESC);
			} else if (querySettings.getSortBy().equals(SortByValue.OLD)) {
				srb.addSort("dcp_updated", SortOrder.ASC);
			}
		}
	}

	/**
	 * @param querySettings
	 * @param srb request builder to set response content for
	 */
	protected void handleResponseContentSettings(QuerySettings querySettings, SearchRequestBuilder srb) {

		// TODO _SEARCH handle 'field' params to return configured fields only. Use defined set of fields by default (loaded
		// from DCP configuration).

		// pagging of results
		QuerySettings.Filters filters = querySettings.getFilters();
		if (filters != null) {
			if (filters.getStart() != null) {
				srb.setFrom(filters.getStart());
			}
			if (filters.getCount() != null) {
				srb.setSize(filters.getCount());
			}
		}
	}

}
