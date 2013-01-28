/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
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
import org.elasticsearch.common.joda.time.format.DateTimeFormatter;
import org.elasticsearch.common.joda.time.format.ISODateTimeFormat;
import org.elasticsearch.common.settings.SettingsException;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.AndFilterBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilteredQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryFilterBuilder;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.index.query.RangeFilterBuilder;
import org.elasticsearch.index.query.TermsFilterBuilder;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.facet.datehistogram.DateHistogramFacetBuilder;
import org.elasticsearch.search.facet.terms.TermsFacetBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.jboss.dcp.api.DcpContentObjectFields;
import org.jboss.dcp.api.annotations.security.GuestAllowed;
import org.jboss.dcp.api.model.FacetValue;
import org.jboss.dcp.api.model.QuerySettings;
import org.jboss.dcp.api.model.QuerySettings.Filters;
import org.jboss.dcp.api.model.SortByValue;
import org.jboss.dcp.api.service.ConfigService;
import org.jboss.dcp.api.service.IndexNamesCacheService;
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

	@Inject
	protected ConfigService configService;

	@Inject
	protected IndexNamesCacheService indexNamesCacheService;

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

			QueryBuilder qb_fulltext = handleFulltextSearchSettings(querySettings);
			Map<String, FilterBuilder> searchFilters = handleCommonFiltersSettings(querySettings);
			srb.setQuery(applyCommonFilters(searchFilters, qb_fulltext));

			searchFilters.put("fulltext_query", new QueryFilterBuilder(qb_fulltext));
			handleFacetSettings(querySettings, searchFilters, srb);

			handleSortingSettings(querySettings, srb);

			handleHighlightSettings(querySettings, srb);

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
			List<String> dcpTypesRequested = null;
			if (querySettings.getFilters() != null) {
				dcpTypesRequested = querySettings.getFilters().getDcpTypes();
			}
			boolean isDcpTypFacet = (querySettings.getFacets() != null && querySettings.getFacets().contains(
					FacetValue.PER_DCP_TYPE_COUNTS));

			Set<String> indexNames = indexNamesCacheService.get(prepareIndexNamesCacheKey(dcpTypesRequested, isDcpTypFacet));
			if (indexNames == null) {
				indexNames = new LinkedHashSet<String>();
				List<Map<String, Object>> allProviders = providerService.listAllProviders();
				for (Map<String, Object> providerCfg : allProviders) {
					try {
						@SuppressWarnings("unchecked")
						Map<String, Map<String, Object>> types = (Map<String, Map<String, Object>>) providerCfg
								.get(ProviderService.TYPE);
						if (types != null) {
							for (String typeName : types.keySet()) {
								Map<String, Object> typeDef = types.get(typeName);
								if ((dcpTypesRequested == null && !ProviderService.extractSearchAllExcluded(typeDef))
										|| (dcpTypesRequested != null && ((isDcpTypFacet && !ProviderService
												.extractSearchAllExcluded(typeDef)) || dcpTypesRequested.contains(ProviderService
												.extractDcpType(typeDef, typeName))))) {
									indexNames.addAll(Arrays.asList(ProviderService.extractSearchIndices(typeDef, typeName)));
								}
							}
						}
					} catch (ClassCastException e) {
						throw new SettingsException("Incorrect configuration of 'type' section for dcp_provider="
								+ providerCfg.get(ProviderService.NAME) + ". Contact administrators please.");
					}
				}
				indexNamesCacheService.put(prepareIndexNamesCacheKey(dcpTypesRequested, isDcpTypFacet), indexNames);
			}
			srb.setIndices(indexNames.toArray(new String[indexNames.size()]));
		}
	}

	/**
	 * Prepare key for indexName cache.
	 * 
	 * @param dcpTypesRequested to prepare key for
	 * @return key value (never null)
	 */
	protected static String prepareIndexNamesCacheKey(List<String> dcpTypesRequested, boolean isDcpTypFacet) {
		if (dcpTypesRequested == null || dcpTypesRequested.isEmpty())
			return "_all||" + isDcpTypFacet;

		if (dcpTypesRequested.size() == 1) {
			return dcpTypesRequested.get(0) + "||" + isDcpTypFacet;
		}

		TreeSet<String> ts = new TreeSet<String>(dcpTypesRequested);
		StringBuilder sb = new StringBuilder();
		for (String k : ts) {
			sb.append(k).append("|");
		}
		sb.append("|").append(isDcpTypFacet);
		return sb.toString();
	}

	/**
	 * @param querySettings
	 * @return builder for query, newer null
	 */
	protected QueryBuilder handleFulltextSearchSettings(QuerySettings querySettings) {
		if (querySettings.getQuery() != null) {
			QueryStringQueryBuilder qb = QueryBuilders.queryString(querySettings.getQuery());
			Map<String, Object> fields = configService.get(ConfigService.CFGNAME_SEARCH_FULLTEXT_QUERY_FIELDS);
			if (fields != null) {
				for (String fieldName : fields.keySet()) {
					String value = (String) fields.get(fieldName);
					if (value != null && !value.trim().isEmpty()) {
						try {
							qb.field(fieldName, Float.parseFloat(value));
						} catch (NumberFormatException e) {
							log.warning("Boost value has not valid float format for fulltext field " + fieldName
									+ " in DCP configuration document " + ConfigService.CFGNAME_SEARCH_FULLTEXT_QUERY_FIELDS);
							qb.field(fieldName);
						}
					} else {
						qb.field(fieldName);
					}
				}
			}
			return qb;
		} else {
			return QueryBuilders.matchAllQuery();
		}
	}

	protected void handleHighlightSettings(QuerySettings querySettings, SearchRequestBuilder srb) {
		if (querySettings.getQuery() != null && querySettings.isQueryHighlight()) {
			Map<String, Object> hf = configService.get(ConfigService.CFGNAME_SEARCH_FULLTEXT_HIGHLIGHT_FIELDS);
			if (hf != null && !hf.isEmpty()) {
				srb.setHighlighterPreTags("<span class='hlt'>");
				srb.setHighlighterPostTags("</span>");
				for (String fieldName : hf.keySet()) {
					srb.addHighlightedField(fieldName, parseHighlightSettingIntParam(hf, fieldName, "fragment_size"),
							parseHighlightSettingIntParam(hf, fieldName, "number_of_fragments"),
							parseHighlightSettingIntParam(hf, fieldName, "fragment_offset"));
				}
			} else {
				throw new SettingsException(
						"Fulltext search highlight requested but not configured by DCP configuration document "
								+ ConfigService.CFGNAME_SEARCH_FULLTEXT_HIGHLIGHT_FIELDS + ". Contact administrators please.");
			}
		}
	}

	@SuppressWarnings("unchecked")
	protected int parseHighlightSettingIntParam(Map<String, Object> highlightConfigStructure, String fieldName,
			String paramName) {
		try {
			Map<String, Object> fieldConfig = (Map<String, Object>) highlightConfigStructure.get(fieldName);
			try {
				Object o = fieldConfig.get(paramName);
				if (o instanceof Integer) {
					return ((Integer) o).intValue();
				} else {
					return Integer.parseInt(o.toString());
				}
			} catch (Exception e) {
				throw new SettingsException("Missing or incorrect configuration of fulltext search highlight field '"
						+ fieldName + "' parameter '" + paramName + "' in DCP configuration document "
						+ ConfigService.CFGNAME_SEARCH_FULLTEXT_HIGHLIGHT_FIELDS + ". Contact administrators please.");
			}
		} catch (ClassCastException e) {
			throw new SettingsException("Incorrect configuration of fulltext search highlight field '" + fieldName
					+ "' in DCP configuration document " + ConfigService.CFGNAME_SEARCH_FULLTEXT_HIGHLIGHT_FIELDS
					+ ". Contact administrators please.");
		}
	}

	private static final DateTimeFormatter DATE_TIME_FORMATTER_UTC = ISODateTimeFormat.dateTime().withZoneUTC();
	/**
	 * Maximal size of response.
	 */
	public static final int RESPONSE_MAX_SIZE = 500;

	/**
	 * @param querySettings
	 * @return filter builder if some filters are here, null if not filters necessary
	 */
	protected Map<String, FilterBuilder> handleCommonFiltersSettings(QuerySettings querySettings) {
		QuerySettings.Filters filters = querySettings.getFilters();
		Map<String, FilterBuilder> searchFilters = new LinkedHashMap<String, FilterBuilder>();

		if (filters != null) {
			addFilter(searchFilters, DcpContentObjectFields.DCP_TYPE, filters.getDcpTypes());
			addFilter(searchFilters, DcpContentObjectFields.DCP_CONTENT_PROVIDER, filters.getDcpContentProvider());
			addFilter(searchFilters, DcpContentObjectFields.DCP_TAGS, filters.getTags());
			addFilter(searchFilters, DcpContentObjectFields.DCP_PROJECT, filters.getProjects());
			addFilter(searchFilters, DcpContentObjectFields.DCP_CONTRIBUTORS, filters.getContributors());
			if (filters.getActivityDateInterval() != null) {
				RangeFilterBuilder range = new RangeFilterBuilder(DcpContentObjectFields.DCP_ACTIVITY_DATES);
				range.from(DATE_TIME_FORMATTER_UTC.print(filters.getActivityDateInterval().getFromTimestamp())).includeLower(
						true);
				searchFilters.put(DcpContentObjectFields.DCP_ACTIVITY_DATES, range);
			} else if (filters.getActivityDateFrom() != null || filters.getActivityDateTo() != null) {
				RangeFilterBuilder range = new RangeFilterBuilder(DcpContentObjectFields.DCP_ACTIVITY_DATES);
				if (filters.getActivityDateFrom() != null) {
					range.from(DATE_TIME_FORMATTER_UTC.print(filters.getActivityDateFrom())).includeLower(true);
				}
				if (filters.getActivityDateTo() != null) {
					range.to(DATE_TIME_FORMATTER_UTC.print(filters.getActivityDateTo())).includeUpper(true);
				}
				searchFilters.put(DcpContentObjectFields.DCP_ACTIVITY_DATES, range);
			}
		}
		return searchFilters;
	}

	protected QueryBuilder applyCommonFilters(Map<String, FilterBuilder> searchFilters, QueryBuilder qb) {
		if (!searchFilters.isEmpty()) {
			return new FilteredQueryBuilder(qb, new AndFilterBuilder(searchFilters.values().toArray(
					new FilterBuilder[searchFilters.size()])));
		} else {
			return qb;
		}
	}

	private void addFilter(Map<String, FilterBuilder> searchFilters, String filterField, List<String> filterValue) {
		if (filterValue != null && !filterValue.isEmpty()) {
			searchFilters.put(filterField, new TermsFilterBuilder(filterField, filterValue));
		}
	}

	private void addFilter(Map<String, FilterBuilder> searchFilters, String filterField, String filterValue) {
		if (filterValue != null && !filterValue.isEmpty()) {
			searchFilters.put(filterField, new TermsFilterBuilder(filterField, filterValue));
		}
	}

	/**
	 * @param querySettings
	 * @param srb
	 */
	protected void handleFacetSettings(QuerySettings querySettings, Map<String, FilterBuilder> searchFilters,
			SearchRequestBuilder srb) {
		Set<FacetValue> facets = querySettings.getFacets();
		if (facets != null) {
			if (facets.contains(FacetValue.PER_PROJECT_COUNTS)) {
				srb.addFacet(createTermsFacetBuilder(FacetValue.PER_PROJECT_COUNTS, DcpContentObjectFields.DCP_PROJECT, 500,
						searchFilters));
			}
			if (facets.contains(FacetValue.PER_DCP_TYPE_COUNTS)) {
				srb.addFacet(createTermsFacetBuilder(FacetValue.PER_DCP_TYPE_COUNTS, DcpContentObjectFields.DCP_TYPE, 20,
						searchFilters));
			}
			if (facets.contains(FacetValue.TOP_CONTRIBUTORS)) {
				srb.addFacet(createTermsFacetBuilder(FacetValue.TOP_CONTRIBUTORS, DcpContentObjectFields.DCP_CONTRIBUTORS, 100,
						searchFilters));
				if (searchFilters != null && searchFilters.containsKey(DcpContentObjectFields.DCP_CONTRIBUTORS)) {
					// we filter over contributors so we have to add second facet which provide numbers for these contributors
					// because they can be out of normal facet due count limitation
					TermsFacetBuilder tb = new TermsFacetBuilder(FacetValue.TOP_CONTRIBUTORS + "_filter")
							.field(DcpContentObjectFields.DCP_CONTRIBUTORS).size(30).global(true)
							.facetFilter(new AndFilterBuilder(getFilters(searchFilters, null)));
					srb.addFacet(tb);
				}

			}
			if (facets.contains(FacetValue.TAG_CLOUD)) {
				srb.addFacet(createTermsFacetBuilder(FacetValue.TAG_CLOUD, DcpContentObjectFields.DCP_TAGS, 50, searchFilters));
			}
			if (facets.contains(FacetValue.ACTIVITY_DATES_HISTOGRAM)) {
				srb.addFacet(new DateHistogramFacetBuilder(FacetValue.ACTIVITY_DATES_HISTOGRAM.toString()).field(
						DcpContentObjectFields.DCP_ACTIVITY_DATES).interval(selectActivityDatesHistogramInterval(querySettings)));
			}
		}
	}

	protected TermsFacetBuilder createTermsFacetBuilder(FacetValue facetName, String facetField, int size,
			Map<String, FilterBuilder> searchFilters) {

		TermsFacetBuilder tb = new TermsFacetBuilder(facetName.toString()).field(facetField).size(size).global(true);
		if (searchFilters != null && !searchFilters.isEmpty()) {
			FilterBuilder[] fb = getFilters(searchFilters, facetField);
			if (fb != null && fb.length > 0)
				tb.facetFilter(new AndFilterBuilder(fb));
		}
		return tb;
	}

	protected static String selectActivityDatesHistogramInterval(QuerySettings querySettings) {
		Filters filters = querySettings.getFilters();
		if (filters != null) {
			if (filters.getActivityDateInterval() != null) {
				switch (filters.getActivityDateInterval()) {
				case YEAR:
				case QUARTER:
					return "week";
				case MONTH:
				case WEEK:
					return "day";
				case DAY:
					return "hour";
				}
			} else if (filters.getActivityDateFrom() != null || filters.getActivityDateTo() != null) {
				long from = 0;
				long to = 0;
				if (filters.getActivityDateTo() != null) {
					to = filters.getActivityDateTo().longValue();
				} else {
					to = System.currentTimeMillis();
				}
				if (filters.getActivityDateFrom() != null) {
					from = filters.getActivityDateFrom().longValue();
				}
				long interval = to - from;
				if (interval < 1000L * 60L * 60L) {
					return "minute";
				} else if (interval < 1000L * 60L * 60L * 24 * 2) {
					return "hour";
				} else if (interval < 1000L * 60L * 60L * 24 * 7 * 8) {
					return "day";
				} else if (interval < 1000L * 60L * 60L * 24 * 366) {
					return "week";
				}
			}
		}
		return "month";
	}

	protected static FilterBuilder[] getFilters(Map<String, FilterBuilder> filters, String filterToExclude) {
		List<FilterBuilder> builders = new ArrayList<FilterBuilder>();
		if (filters != null) {
			for (String name : filters.keySet()) {
				if (filterToExclude == null || !filterToExclude.equals(name)) {
					builders.add(filters.get(name));
				}
			}
		}
		return builders.toArray(new FilterBuilder[builders.size()]);
	}

	/**
	 * @param querySettings
	 * @param srb request builder to set sorting for
	 */
	protected void handleSortingSettings(QuerySettings querySettings, SearchRequestBuilder srb) {
		if (querySettings.getSortBy() != null) {
			if (querySettings.getSortBy().equals(SortByValue.NEW)) {
				srb.addSort(DcpContentObjectFields.DCP_LAST_ACTIVITY_DATE, SortOrder.DESC);
			} else if (querySettings.getSortBy().equals(SortByValue.OLD)) {
				srb.addSort(DcpContentObjectFields.DCP_LAST_ACTIVITY_DATE, SortOrder.ASC);
			}
		}
	}

	/**
	 * @param querySettings
	 * @param srb request builder to set response content for
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected void handleResponseContentSettings(QuerySettings querySettings, SearchRequestBuilder srb) {

		// handle 'field' params to return configured fields only. Use default set of fields loaded from DCP configuration.
		if (querySettings.getFields() != null) {
			srb.addFields((querySettings.getFields()).toArray(new String[querySettings.getFields().size()]));
		} else {
			Map<String, Object> cf = configService.get(ConfigService.CFGNAME_SEARCH_RESPONSE_FIELDS);
			if (cf != null && cf.containsKey(ConfigService.CFGNAME_SEARCH_RESPONSE_FIELDS)) {
				Object o = cf.get(ConfigService.CFGNAME_SEARCH_RESPONSE_FIELDS);
				if (o instanceof Collection) {
					srb.addFields(((Collection<String>) o).toArray(new String[((Collection) o).size()]));
				} else if (o instanceof String) {
					srb.addField((String) o);
				} else {
					throw new SettingsException(ConfigService.CFGNAME_SEARCH_RESPONSE_FIELDS
							+ " DCP configuration document is invalid. Contact administrators please.");
				}
			}
		}

		// paging of results
		QuerySettings.Filters filters = querySettings.getFilters();
		if (filters != null) {
			if (filters.getFrom() != null && filters.getFrom() >= 0) {
				srb.setFrom(filters.getFrom());
			}
			if (filters.getSize() != null && filters.getSize() >= 0) {
				int size = filters.getSize();
				if (size > RESPONSE_MAX_SIZE)
					size = RESPONSE_MAX_SIZE;
				srb.setSize(size);
			}
		}
	}
}
