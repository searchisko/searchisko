/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.Singleton;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.settings.SettingsException;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.AndFilterBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilteredQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryFilterBuilder;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.facet.datehistogram.DateHistogramFacetBuilder;
import org.elasticsearch.search.facet.terms.TermsFacetBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.searchisko.api.ContentObjectFields;
import org.searchisko.api.cache.IndexNamesCache;
import org.searchisko.api.model.QuerySettings;
import org.searchisko.api.model.SortByValue;
import org.searchisko.api.model.TimeoutConfiguration;
import org.searchisko.api.rest.exception.NotAuthorizedException;
import org.searchisko.api.rest.search.SemiParsedFacetConfig;
import org.searchisko.api.service.ProviderService.ProviderContentTypeInfo;

import static org.searchisko.api.rest.search.ConfigParseUtil.parseFacetType;

/**
 * Search business logic service.
 * 
 * @author Libor Krzyzanek
 * @author Vlastimil Elias (velias at redhat dot com)
 * @author Lukas Vlcek
 */
@Named
@ApplicationScoped
@Singleton
public class SearchService {

	@Inject
	protected SearchClientService searchClientService;

	@Inject
	protected StatsClientService statsClientService;

	@Inject
	protected ProviderService providerService;

	@Inject
	protected ConfigService configService;

	@Inject
	protected ParsedFilterConfigService parsedFilterConfigService;

	@Inject
	protected IndexNamesCache indexNamesCache;

	@Inject
	protected TimeoutConfiguration timeout;

	@Inject
	protected AuthenticationUtilService authenticationUtilService;

	@Inject
	protected Logger log;

	/**
	 * Perform search operation.
	 * 
	 * @param querySettings to use for search
	 * @param responseUuid used for search response, we need it only to write it into statistics (so can be null)
	 * @param statsRecordType
	 * @return search response
	 */
	public SearchResponse performSearch(QuerySettings querySettings, String responseUuid, StatsRecordType statsRecordType) {
		try {

			SearchRequestBuilder srb = new SearchRequestBuilder(searchClientService.getClient());
			srb = performSearchInternal(querySettings, srb);
			srb.setTimeout(TimeValue.timeValueSeconds(timeout.search()));

			log.log(Level.FINE, "Elasticsearch Search request: {0}", srb);
			final SearchResponse searchResponse = srb.execute().actionGet();
			statsClientService.writeStatisticsRecord(statsRecordType, responseUuid, searchResponse,
					System.currentTimeMillis(), querySettings);
			return searchResponse;
		} catch (ElasticsearchException e) {
			statsClientService.writeStatisticsRecord(statsRecordType, e, System.currentTimeMillis(), querySettings);
			throw e;
		}
	}

	/**
	 * This method handles search query building. The output is a
	 * {@link org.elasticsearch.action.search.SearchRequestBuilder} entity that reflects input parameters. This method can
	 * be used for testing of final complete Elasticsearch query.
	 * 
	 * @param querySettings
	 * @param srb
	 * @return SearchRequestBuilder entity
	 */
	protected SearchRequestBuilder performSearchInternal(final QuerySettings querySettings, SearchRequestBuilder srb) {
		if (!parsedFilterConfigService.isCacheInitialized()) {
			try {
				parsedFilterConfigService.prepareFiltersForRequest(querySettings.getFilters());
			} catch (ReflectiveOperationException e) {
				throw new ElasticsearchException("Can not prepare filters", e);
			}
		}

		setSearchRequestIndicesAndTypes(querySettings, srb);

		QueryBuilder qb_fulltext = prepareQueryBuilder(querySettings);
		srb.setQuery(applyCommonFilters(parsedFilterConfigService.getSearchFiltersForRequest(), qb_fulltext));

		parsedFilterConfigService.getSearchFiltersForRequest().put("fulltext_query", new QueryFilterBuilder(qb_fulltext)); // ??
		handleFacetSettings(querySettings, parsedFilterConfigService.getSearchFiltersForRequest(), srb);

		setSearchRequestSort(querySettings, srb);
		setSearchRequestHighlight(querySettings, srb);
		setSearchRequestFields(querySettings, srb);
		setSearchRequestFromSize(querySettings, srb);

		return srb;
	}

	/**
	 * Set indices and types into ES search request builder according to the query settings and security constraints.
	 * <p>
	 * <strong>SECURITY NOTE:</strong> this method plays crucial role for "content type level security"! It fills search
	 * request builder with indices and types for content types user has permission to only. So this method MUST BE used
	 * for each search requests for common users! This method uses {@link AuthenticationUtilService}.
	 * 
	 * @param querySettings to
	 * @param srb ES search request builder to add searched indices and types to
	 * @param NotAuthorizedException if current user has not permission to any of content he requested.
	 * 
	 */
	protected void setSearchRequestIndicesAndTypes(QuerySettings querySettings, SearchRequestBuilder srb)
			throws NotAuthorizedException {

		Set<String> contentTypes = null;
		if (querySettings.getFilters() != null && querySettings.getFilters().getFilterCandidatesKeys().size() > 0) {
			Set<String> fn = parsedFilterConfigService.getFilterNamesForDocumentField(ContentObjectFields.SYS_CONTENT_TYPE);
			contentTypes = querySettings.getFilters().getFilterCandidateValues(fn);
		}

		Set<String> allQueryIndices = null;
		Set<String> allQueryTypes = null;

		if (contentTypes != null && contentTypes.size() > 0) {
			allQueryIndices = new LinkedHashSet<>();
			allQueryTypes = new LinkedHashSet<>();
			for (String type : contentTypes) {
				ProviderContentTypeInfo typeDef = providerService.findContentType(type);
				if (typeDef == null) {
					throw new IllegalArgumentException("Unsupported content type");
				}
				// #142 - check content type level security there
				Collection<String> roles = ProviderService.extractTypeVisibilityRoles(typeDef, type);
				if (roles == null || authenticationUtilService.isUserInAnyOfRoles(true, roles)) {
					String[] queryIndices = ProviderService.extractSearchIndices(typeDef, type);
					String queryType = ProviderService.extractIndexType(typeDef, type);
					if (log.isLoggable(Level.FINE)) {
						log.log(Level.FINE, "Query indices and types relevant to {0}: {1}", new Object[] {
								ContentObjectFields.SYS_CONTENT_TYPE, type });
						log.log(Level.FINE, "Query indices: {0}", Arrays.asList(queryIndices).toString());
						log.log(Level.FINE, "Query indices type: {0}", queryType);
					}
					Collections.addAll(allQueryIndices, queryIndices);
					allQueryTypes.add(queryType);
				}
			}
		} else {
			Set<String> sysTypesRequested = null;
			if (querySettings.getFilters() != null && querySettings.getFilters().getFilterCandidatesKeys().size() > 0) {
				Set<String> fn = parsedFilterConfigService.getFilterNamesForDocumentField(ContentObjectFields.SYS_TYPE);
				sysTypesRequested = querySettings.getFilters().getFilterCandidateValues(fn);
			}
			boolean isSysTypeFacet = (querySettings.getFacets() != null && querySettings.getFacets().contains(
					getFacetNameUsingSysTypeField()));

			// #142 - we can't cache for authenticated users due content type level security
			String indexNameCacheKey = null;
			if (!authenticationUtilService.isAuthenticatedUser()) {
				indexNameCacheKey = prepareIndexNamesCacheKey(sysTypesRequested, isSysTypeFacet);
				allQueryIndices = indexNamesCache.get(indexNameCacheKey);
			}
			if (allQueryIndices == null) {
				allQueryIndices = prepareIndexNamesForSysType(sysTypesRequested, isSysTypeFacet);
				if (indexNameCacheKey != null) {
					indexNamesCache.put(indexNameCacheKey, allQueryIndices);
				}
			}
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "Query indices: {0}", allQueryIndices);
			}
		}

		if ((allQueryIndices == null || allQueryIndices.isEmpty()) && (allQueryTypes == null || allQueryTypes.isEmpty())) {
			throw new NotAuthorizedException("No content available for current user");
		}

		if (allQueryIndices != null && !allQueryIndices.isEmpty())
			srb.setIndices(allQueryIndices.toArray(new String[allQueryIndices.size()]));
		if (allQueryTypes != null && !allQueryTypes.isEmpty())
			srb.setTypes(allQueryTypes.toArray(new String[allQueryTypes.size()]));

	}

	/**
	 * Prepare key for indexName cache.
	 * 
	 * @param sysTypesRequested to prepare key for
	 * @param isSysTypeFacet
	 * @return key value (never null)
	 */
	protected static String prepareIndexNamesCacheKey(Set<String> sysTypesRequested, boolean isSysTypeFacet) {
		if (sysTypesRequested == null || sysTypesRequested.isEmpty())
			return "_all||" + isSysTypeFacet;

		if (sysTypesRequested.size() == 1) {
			return sysTypesRequested.iterator().next() + "||" + isSysTypeFacet;
		}

		List<String> ordered = new ArrayList<>(sysTypesRequested);
		Collections.sort(ordered);
		StringBuilder sb = new StringBuilder();
		for (String k : ordered) {
			sb.append(k).append("|");
		}
		sb.append("|").append(isSysTypeFacet);
		return sb.toString();
	}

	private Set<String> prepareIndexNamesForSysType(Set<String> sysTypesRequested, boolean isSysTypeFacet) {
		if (sysTypesRequested != null && sysTypesRequested.isEmpty())
			sysTypesRequested = null;
		Set<String> indexNames = new LinkedHashSet<>();
		List<Map<String, Object>> allProviders = providerService.getAll();
		boolean unknownType = true;
		boolean isAnyType = false;
		for (Map<String, Object> providerCfg : allProviders) {
			try {
				@SuppressWarnings("unchecked")
				Map<String, Map<String, Object>> types = (Map<String, Map<String, Object>>) providerCfg
						.get(ProviderService.TYPE);
				if (types != null) {
					for (String typeName : types.keySet()) {
						isAnyType = true;
						Map<String, Object> typeDef = types.get(typeName);
						// #142 - check content type level security there
						Collection<String> roles = ProviderService.extractTypeVisibilityRoles(typeDef, typeName);
						if (roles == null || authenticationUtilService.isUserInAnyOfRoles(true, roles)) {
							String sysType = ProviderService.extractSysType(typeDef, typeName);
							if ((sysTypesRequested == null && !ProviderService.extractSearchAllExcluded(typeDef))
									|| (sysTypesRequested != null && ((isSysTypeFacet && !ProviderService
											.extractSearchAllExcluded(typeDef)) || sysTypesRequested.contains(sysType)))) {
								indexNames.addAll(Arrays.asList(ProviderService.extractSearchIndices(typeDef, typeName)));
							} else if (sysTypesRequested == null || sysTypesRequested.contains(sysType)) {
								unknownType = false;
							}
						}
					}
				}
			} catch (ClassCastException e) {
				throw new SettingsException("Incorrect configuration of 'type' section for sys_provider="
						+ providerCfg.get(ProviderService.NAME) + ".");
			}
		}

		if (!isAnyType) {
			throw new SettingsException("No any content type available");
		}

		if (sysTypesRequested != null && indexNames.isEmpty() && unknownType) {
			throw new IllegalArgumentException("Unsupported content sys_type");
		}

		return indexNames;
	}

	/**
	 * Prepare query builder based on query settings.
	 * 
	 * Under the hood it creates either {@link org.elasticsearch.index.query.QueryStringQueryBuilder} using fields
	 * configured in {@link ConfigService#CFGNAME_SEARCH_FULLTEXT_QUERY_FIELDS} config file or
	 * {@link org.elasticsearch.index.query.MatchAllQueryBuilder} if query string is <code>null</code>.
	 * 
	 * @param querySettings
	 * @return builder for query, never null
	 */
	protected QueryBuilder prepareQueryBuilder(QuerySettings querySettings) {
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
									+ " in configuration document " + ConfigService.CFGNAME_SEARCH_FULLTEXT_QUERY_FIELDS);
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

	/**
	 * @param querySettings
	 * @param srb
	 * @see <a
	 *      href="http://www.elasticsearch.org/guide/en/elasticsearch/reference/0.90/search-request-highlighting.html">Elasticsearch
	 *      0.90 - Highlighting</a>
	 */
	protected void setSearchRequestHighlight(QuerySettings querySettings, SearchRequestBuilder srb) {
		if (querySettings.getQuery() != null && querySettings.isQueryHighlight()) {
			Map<String, Object> hf = configService.get(ConfigService.CFGNAME_SEARCH_FULLTEXT_HIGHLIGHT_FIELDS);
			if (hf != null && !hf.isEmpty()) {
				srb.setHighlighterPreTags("<span class='hlt'>");
				srb.setHighlighterPostTags("</span>");
				srb.setHighlighterEncoder("html");
				for (String fieldName : hf.keySet()) {
					srb.addHighlightedField(fieldName, parseHighlightSettingIntParam(hf, fieldName, "fragment_size"),
							parseHighlightSettingIntParam(hf, fieldName, "number_of_fragments"),
							parseHighlightSettingIntParam(hf, fieldName, "fragment_offset"));
				}
			} else {
				throw new SettingsException("Fulltext search highlight requested but not configured by configuration document "
						+ ConfigService.CFGNAME_SEARCH_FULLTEXT_HIGHLIGHT_FIELDS + ".");
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
						+ fieldName + "' parameter '" + paramName + "' in configuration document "
						+ ConfigService.CFGNAME_SEARCH_FULLTEXT_HIGHLIGHT_FIELDS + ".");
			}
		} catch (ClassCastException e) {
			throw new SettingsException("Incorrect configuration of fulltext search highlight field '" + fieldName
					+ "' in configuration document " + ConfigService.CFGNAME_SEARCH_FULLTEXT_HIGHLIGHT_FIELDS + ".");
		}
	}

	/**
	 * Maximal size of response.
	 */
	public static final int RESPONSE_MAX_SIZE = 500;

	protected QueryBuilder applyCommonFilters(Map<String, FilterBuilder> searchFilters, QueryBuilder qb) {
		if (!searchFilters.isEmpty()) {
			return new FilteredQueryBuilder(qb, new AndFilterBuilder(searchFilters.values().toArray(
					new FilterBuilder[searchFilters.size()])));
		} else {
			return qb;
		}
	}

	/**
	 * @param querySettings
	 * @param srb
	 */
	protected void handleFacetSettings(QuerySettings querySettings, Map<String, FilterBuilder> searchFilters,
			SearchRequestBuilder srb) {
		Map<String, Object> configuredFacets = configService.get(ConfigService.CFGNAME_SEARCH_FULLTEXT_FACETS_FIELDS);
		Set<String> requestedFacets = querySettings.getFacets();
		if (configuredFacets != null && !configuredFacets.isEmpty() && requestedFacets != null
				&& !requestedFacets.isEmpty()) {
			for (String requestedFacet : requestedFacets) {
				Object facetConfig = configuredFacets.get(requestedFacet);
				if (facetConfig != null) {
					SemiParsedFacetConfig parsedFacetConfig = parseFacetType(facetConfig, requestedFacet);
					// terms facet
					if (SemiParsedFacetConfig.FacetType.TERMS.toString().equals(parsedFacetConfig.getFacetType())) {
						int size;
						try {
							size = (int) parsedFacetConfig.getOptionalSettings().get("size");
						} catch (Exception e) {
							throw new SettingsException("Incorrect configuration of fulltext search facet field '" + requestedFacet
									+ "' in configuration document " + ConfigService.CFGNAME_SEARCH_FULLTEXT_FACETS_FIELDS
									+ ": Invalid value of [size] field.");
						}
						srb.addFacet(createTermsFacetBuilder(requestedFacet, parsedFacetConfig.getFieldName(), size, searchFilters));
						if (searchFilters != null && searchFilters.containsKey(parsedFacetConfig.getFieldName())) {
							if (parsedFacetConfig.isFiltered()) {
								// we filter over contributors so we have to add second facet which provide numbers for these
								// contributors because they can be out of normal facet due count limitation
								TermsFacetBuilder tb = new TermsFacetBuilder(requestedFacet + "_filter")
										.field(parsedFacetConfig.getFieldName()).size(parsedFacetConfig.getFilteredSize()).global(true)
										.facetFilter(new AndFilterBuilder(filtersMapToArray(searchFilters)));
								srb.addFacet(tb);
							}
						}
						// date histogram facet
					} else if (SemiParsedFacetConfig.FacetType.DATE_HISTOGRAM.toString().equals(parsedFacetConfig.getFacetType())) {
						srb.addFacet(new DateHistogramFacetBuilder(requestedFacet).field(parsedFacetConfig.getFieldName())
								.interval(getDateHistogramFacetInterval(parsedFacetConfig.getFieldName())));
					}
				}
			}
		}
	}

	/**
	 * Return (the first) name of fact that is built on top of "sys_type" field.
	 * 
	 * @return (the first) name of fact that is built on top of "sys_type" field.
	 */
	private String getFacetNameUsingSysTypeField() {
		Map<String, Object> configuredFacets = configService.get(ConfigService.CFGNAME_SEARCH_FULLTEXT_FACETS_FIELDS);
		if (configuredFacets != null && !configuredFacets.isEmpty()) {
			for (String facetName : configuredFacets.keySet()) {
				Object facetConfig = configuredFacets.get(facetName);
				if (facetConfig != null) {
					SemiParsedFacetConfig config = parseFacetType(facetConfig, facetName);
					if (ContentObjectFields.SYS_TYPE.equals(config.getFieldName())) {
						return facetName;
					}
				}
			}
		}
		return "";
	}

	/**
	 * For given set of facet names it returns only those using "date_histogram" facet type. It also returns name of their
	 * document filed.
	 * 
	 * @param facetNames set of facet names to filter
	 * @return only those facets names using "date_histogram" facet type
	 */
	private Map<String, String> filterFacetNamesUsingDateHistogramFacetType(Set<String> facetNames) {
		Map<String, String> result = new HashMap<>();
		if (facetNames.size() > 0) {
			Map<String, Object> configuredFacets = configService.get(ConfigService.CFGNAME_SEARCH_FULLTEXT_FACETS_FIELDS);
			if (configuredFacets != null && !configuredFacets.isEmpty()) {
				for (String facetName : configuredFacets.keySet()) {
					Object facetConfig = configuredFacets.get(facetName);
					if (facetConfig != null) {
						SemiParsedFacetConfig config = parseFacetType(facetConfig, facetName);
						if (SemiParsedFacetConfig.FacetType.DATE_HISTOGRAM.toString().equals(config.getFacetType())) {
							result.put(facetName, config.getFieldName());
						}
					}
				}
			}
		}
		return result;
	}

	/**
	 * Get interval values for Date Histogram facets.
	 * 
	 * @param querySettings for search
	 * @return map with additional fields, never null
	 */
	public Map<String, String> getIntervalValuesForDateHistogramFacets(QuerySettings querySettings) {
		Map<String, String> ret = new HashMap<>();
		Set<String> facets = querySettings.getFacets();
		if (facets != null && !facets.isEmpty()) {
			Map<String, String> dateHistogramFacets = filterFacetNamesUsingDateHistogramFacetType(facets);
			for (String facetName : dateHistogramFacets.keySet()) {
				String interval = getDateHistogramFacetInterval(dateHistogramFacets.get(facetName));
				if (interval != null) {
					ret.put(facetName + "_interval", interval);
				}
			}
		}
		return ret;
	}

	protected TermsFacetBuilder createTermsFacetBuilder(String facetName, String facetField, int size,
			Map<String, FilterBuilder> searchFilters) {

		TermsFacetBuilder tb = new TermsFacetBuilder(facetName).field(facetField).size(size).global(true);
		if (searchFilters != null && !searchFilters.isEmpty()) {
			FilterBuilder[] fb = filtersMapToArrayExcluding(searchFilters, facetField);
			if (fb != null && fb.length > 0)
				tb.facetFilter(new AndFilterBuilder(fb));
		}
		return tb;
	}

	/**
	 * 
	 * @param fieldName
	 * @return interval value or null
	 */
	protected String getDateHistogramFacetInterval(String fieldName) {
		String defaultValue = "month";
		if (parsedFilterConfigService.isCacheInitialized()) {
			ParsedFilterConfigService.IntervalRange ir = parsedFilterConfigService.getRangeFiltersIntervals().get(fieldName);
			if (ir != null) {
				long from = 0;
				long to = System.currentTimeMillis();
				if (ir.getGte() != null)
					from = ir.getGte().toDate().getTime();
				if (ir.getLte() != null)
					to = ir.getLte().toDate().getTime();
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
		return defaultValue;
	}

	protected static FilterBuilder[] filtersMapToArray(Map<String, FilterBuilder> filters) {
		return filtersMapToArrayExcluding(filters, null);
	}

	protected static FilterBuilder[] filtersMapToArrayExcluding(Map<String, FilterBuilder> filters, String filterToExclude) {
		List<FilterBuilder> builders = new ArrayList<>();
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
	 * @see <a
	 *      href="http://www.elasticsearch.org/guide/en/elasticsearch/reference/0.90/search-request-sort.html">Elasticsearch
	 *      0.90 - Sort</a>
	 */
	protected void setSearchRequestSort(QuerySettings querySettings, SearchRequestBuilder srb) {
		if (querySettings.getSortBy() != null) {
			if (querySettings.getSortBy().equals(SortByValue.NEW)) {
				srb.addSort(ContentObjectFields.SYS_LAST_ACTIVITY_DATE, SortOrder.DESC);
			} else if (querySettings.getSortBy().equals(SortByValue.OLD)) {
				srb.addSort(ContentObjectFields.SYS_LAST_ACTIVITY_DATE, SortOrder.ASC);
			} else if (querySettings.getSortBy().equals(SortByValue.NEW_CREATION)) {
				srb.addSort(ContentObjectFields.SYS_CREATED, SortOrder.DESC);
			}
		}
	}

	/**
	 * @param querySettings
	 * @param srb request builder to set response content for
	 * @see <a
	 *      href="http://www.elasticsearch.org/guide/en/elasticsearch/reference/0.90/search-request-fields.html">Elasticsearch
	 *      0.90 - Fields</a>
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected void setSearchRequestFields(QuerySettings querySettings, SearchRequestBuilder srb) {

		// handle 'field' params to return configured fields only. Use default set of fields loaded from configuration.
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
							+ " configuration document is invalid.");
				}
			}
		}
	}

	/**
	 * @param querySettings
	 * @param srb
	 * @link <a href="http://www.elasticsearch.org/guide/en/elasticsearch/reference/0.90/search-request-from-size.html">
	 *       Elasticsearch 0.90 - From/Size</a>
	 */
	protected void setSearchRequestFromSize(QuerySettings querySettings, SearchRequestBuilder srb) {
		if (querySettings.getFrom() != null && querySettings.getFrom() >= 0) {
			srb.setFrom(querySettings.getFrom());
		}
		if (querySettings.getSize() != null && querySettings.getSize() >= 0) {
			int size = querySettings.getSize();
			if (size > RESPONSE_MAX_SIZE)
				size = RESPONSE_MAX_SIZE;
			srb.setSize(size);
		}
	}

	/**
	 * Write info about used search hit into statistics. Validation is performed inside of this method to ensure given
	 * content was returned as hit of given search response.
	 * 
	 * @param uuid of search response hit was returned in
	 * @param contentId identifier of content used
	 * @param sessionId optional session id
	 * @return true if validation was successful so record was written
	 */
	public boolean writeSearchHitUsedStatisticsRecord(String uuid, String contentId, String sessionId) {
		Map<String, Object> conditions = new HashMap<String, Object>();
		conditions.put(StatsClientService.FIELD_RESPONSE_UUID, uuid);
		conditions.put(StatsClientService.FIELD_HITS_ID, contentId);
		if (statsClientService.checkStatisticsRecordExists(StatsRecordType.SEARCH, conditions)) {
			if (sessionId != null)
				conditions.put("session", sessionId);
			statsClientService.writeStatisticsRecord(StatsRecordType.SEARCH_HIT_USED, System.currentTimeMillis(), conditions);
			return true;
		} else {
			return false;
		}
	}
}
