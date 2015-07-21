/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.Lock;
import javax.ejb.LockType;
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
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.FilteredQueryBuilder;
import org.elasticsearch.index.query.OrFilterBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryFilterBuilder;
import org.elasticsearch.index.query.SimpleQueryStringBuilder;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.global.GlobalBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogram;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.searchisko.api.ContentObjectFields;
import org.searchisko.api.cache.IndexNamesCache;
import org.searchisko.api.model.QuerySettings;
import org.searchisko.api.model.SortByValue;
import org.searchisko.api.model.TimeoutConfiguration;
import org.searchisko.api.rest.exception.BadFieldException;
import org.searchisko.api.rest.exception.NotAuthorizedException;
import org.searchisko.api.rest.search.SemiParsedAggregationConfig;
import org.searchisko.api.security.Role;
import org.searchisko.api.service.ProviderService.ProviderContentTypeInfo;
import org.searchisko.api.util.SearchUtils;

import static org.searchisko.api.rest.search.ConfigParseUtil.parseAggregationType;

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
@Lock(LockType.READ)
public class SearchService {

	public static final String CFGNAME_FIELD_VISIBLE_FOR_ROLES = "field_visible_for_roles";
	public static final String CFGNAME_SOURCE_FILTERING_FOR_ROLES = "source_filtering_for_roles";

	// This value is used as a key in aggregation filter map. It should be "unique" enough so that it will not
	// clash with any real key value used in configuration files.
	private static final String QUERY_FILTER_KEY = "_query_filter_key";

	@Inject
	protected SearchClientService searchClientService;

	@Inject
	protected StatsClientService statsClientService;

	@Inject
	protected ProviderService providerService;

	@Inject
	protected RegisteredQueryService registeredQueryService;

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
	 * This method handles search query building.
	 * 
	 * @param querySettings
	 * @param srb
	 * @return SearchRequestBuilder {@link org.elasticsearch.action.search.SearchRequestBuilder} instance that reflects input parameters
	 */
	protected SearchRequestBuilder performSearchInternal(final QuerySettings querySettings, SearchRequestBuilder srb) {
		if (!parsedFilterConfigService.isCacheInitialized()) {
			try {
				parsedFilterConfigService.prepareFiltersForRequest(querySettings.getFilters());
			} catch (ReflectiveOperationException e) {
				throw new ElasticsearchException("Can not prepare filters", e);
			}
		}

		setSearchRequestIndicesAndTypes(querySettings.getFilters(), querySettings.getAggregations(), srb);

		QueryBuilder qb = prepareQueryBuilder(querySettings);
		srb.setQuery(
			applyContentLevelSecurityFilter(
				applyCommonFilters(parsedFilterConfigService.getSearchFiltersForRequest(), qb)
			)
		);

		// In some cases we need to apply also filter based on client input (Query filter).
		// Thus we are adding it here into filters valid for actual request under arbitrary key.
		parsedFilterConfigService.getSearchFiltersForRequest().put(QUERY_FILTER_KEY, new QueryFilterBuilder(qb));

		handleAggregationSettings(querySettings, parsedFilterConfigService.getSearchFiltersForRequest(), srb);

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
	 * <p>
	 * Apart from security constrains this method sets indices and types to SearchRequestBuilder but to make the query
	 * return correct set of data there <strong>MUST</strong> be used appropriate filters in the query itself as well.
	 * The indices and types that are set to SearchRequestBuilder are only "hints" for query execution optimization.
	 * (Elasticsearch combines selected indices and types using OR operator, but we need AND, thus additional filters
	 * in the query itself are usually necessary.)
	 *
	 * @param filters request filters
	 * @param aggregations request aggregations
	 * @param srb ES search request builder to add searched indices and types to
	 * @throws NotAuthorizedException if current user has not permission to any of content he requested.
	 * 
	 */
	protected void setSearchRequestIndicesAndTypes(final QuerySettings.Filters filters, final Set<String> aggregations,
												   final SearchRequestBuilder srb)
			throws NotAuthorizedException {

		Set<String> contentTypes = null;
		if (filters != null && filters.getFilterCandidatesKeys().size() > 0) {
			Set<String> fn = parsedFilterConfigService.getFilterNamesForDocumentField(ContentObjectFields.SYS_CONTENT_TYPE);
			contentTypes = filters.getFilterCandidateValues(fn);
		}

		Set<String> allQueryIndices = null;
		Set<String> allQueryTypes = null;

		if (contentTypes != null && contentTypes.size() > 0) {
			allQueryIndices = new LinkedHashSet<>();
			allQueryTypes = new LinkedHashSet<>();
			populateIndicesAndTypesForUserInRoleBasedOnContentTypes(contentTypes, allQueryIndices, allQueryTypes);
		} else {
			Set<String> sysTypesRequested = null;
			if (filters != null && filters.getFilterCandidatesKeys().size() > 0) {
				Set<String> fn = parsedFilterConfigService.getFilterNamesForDocumentField(ContentObjectFields.SYS_TYPE);
				sysTypesRequested = filters.getFilterCandidateValues(fn);
			}
			boolean isSysTypeAggregation = (aggregations != null && aggregations.contains(
					getAggregationNameUsingSysTypeField()));
			allQueryIndices = getIndicesForUserInRoleBasedOnTypes(sysTypesRequested, isSysTypeAggregation);
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
	 *
	 * @param sysTypesRequested
	 * @param isSysTypeAggregation
	 * @return
	 */
	protected Set<String> getIndicesForUserInRoleBasedOnTypes(Set<String> sysTypesRequested, boolean isSysTypeAggregation) {
        Set<String> allQueryIndices = null;
		// #142 - we can't cache for authenticated users due content type level security
		String indexNameCacheKey = null;
		if (!authenticationUtilService.isAuthenticatedUser()) {
            indexNameCacheKey = prepareIndexNamesCacheKey(sysTypesRequested, isSysTypeAggregation);
            allQueryIndices = indexNamesCache.get(indexNameCacheKey);
        }
		if (allQueryIndices == null) {
            allQueryIndices = prepareIndexNamesForSysType(sysTypesRequested, isSysTypeAggregation);
            if (indexNameCacheKey != null) {
                indexNamesCache.put(indexNameCacheKey, allQueryIndices);
            }
        }
		if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "Query indices: {0}", allQueryIndices);
        }
		return allQueryIndices;
	}

	/**
	 * Populate allowed indices and types based on requested content types into allowedIndices and allowedTypes
	 * respectively according to roles of user. Note it populates values into allowedIndices and allowedTypes
	 * in-place. Note both allowedIndices and allowedTypes are cleared first.
	 *
	 * @param contentTypes
	 * @param allowedIndices
	 * @param allowedTypes
	 */
	private void populateIndicesAndTypesForUserInRoleBasedOnContentTypes(final Collection<String> contentTypes,
																		 final Set<String> allowedIndices,
																		 final Set<String> allowedTypes) {
		if (allowedIndices == null || allowedTypes == null) {
			throw new IllegalArgumentException("Invalid arguments, allowedIndices or allowedTypes is null.");
		}
		allowedIndices.clear();
		allowedTypes.clear();
		if (contentTypes != null) {
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
						log.log(Level.FINE, "Query indices and types relevant to {0}: {1}", new Object[]{
								ContentObjectFields.SYS_CONTENT_TYPE, type});
						log.log(Level.FINE, "Query indices: {0}", Arrays.asList(queryIndices).toString());
						log.log(Level.FINE, "Query indices type: {0}", queryType);
					}
					Collections.addAll(allowedIndices, queryIndices);
					allowedTypes.add(queryType);
				}
			}
		}
	}

	/**
	 * Prepare key for indexName cache.
	 * 
	 * @param sysTypesRequested to prepare key for
	 * @param isSysTypeAggregation
	 * @return key value (never null)
	 */
	protected static String prepareIndexNamesCacheKey(Set<String> sysTypesRequested, boolean isSysTypeAggregation) {
		if (sysTypesRequested == null || sysTypesRequested.isEmpty())
			return "_all||" + isSysTypeAggregation;

		if (sysTypesRequested.size() == 1) {
			return sysTypesRequested.iterator().next() + "||" + isSysTypeAggregation;
		}

		List<String> ordered = new ArrayList<>(sysTypesRequested);
		Collections.sort(ordered);
		StringBuilder sb = new StringBuilder();
		for (String k : ordered) {
			sb.append(k).append("|");
		}
		sb.append("|").append(isSysTypeAggregation);
		return sb.toString();
	}

	private Set<String> prepareIndexNamesForSysType(Set<String> sysTypesRequested, boolean isSysTypeAggregation) {
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
									|| (sysTypesRequested != null && ((isSysTypeAggregation && !ProviderService
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
	 * If user query is provided (it is not null) then SimpleQueryString is used and all configured
	 * fields are set on it. Otherwise MatchAll query is used.
	 * 
	 * Under the hood it creates either {@link org.elasticsearch.index.query.SimpleQueryStringBuilder} using fields
	 * configured in {@link ConfigService#CFGNAME_SEARCH_FULLTEXT_QUERY_FIELDS} config file or
	 * {@link org.elasticsearch.index.query.MatchAllQueryBuilder} if query string is <code>null</code>.
	 * 
	 * @param querySettings
	 * @return builder for query, never null
	 */
	protected QueryBuilder prepareQueryBuilder(QuerySettings querySettings) {
		if (querySettings.getQuery() != null) {
			SimpleQueryStringBuilder qb = QueryBuilders.simpleQueryString(querySettings.getQuery());
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
	 * Apply "document level security" filtering to the query filter. See <a
	 * href="https://github.com/searchisko/searchisko/issues/143">issue #134</a>
	 * 
	 * @param qb to apply additional filter to
	 * @return new query filter with applied security filtering
	 */
	protected QueryBuilder applyContentLevelSecurityFilter(QueryBuilder qb) {
		FilterBuilder securityFilter = getContentLevelSecurityFilterInternal();
		if (securityFilter == null) {
			return qb;
		} else {
			return new FilteredQueryBuilder(qb, securityFilter);
		}
	}

	/**
	 * @return security filter or null (if no filters apply for authenticated user)
	 */
	protected FilterBuilder getContentLevelSecurityFilterInternal() {

		if (authenticationUtilService.isUserInRole(Role.ADMIN)) return null;

		List<FilterBuilder> filters = new ArrayList<>();
		filters.add(FilterBuilders.missingFilter(ContentObjectFields.SYS_VISIBLE_FOR_ROLES).existence(true).nullValue(true));

		if (authenticationUtilService.isAuthenticatedUser()) {
			Set<String> roles = authenticationUtilService.getUserRoles();
			if (roles != null && !roles.isEmpty()) {
				filters.add(FilterBuilders.termsFilter(ContentObjectFields.SYS_VISIBLE_FOR_ROLES, roles));
			}
		}

		FilterBuilder securityFilter = null;
		if (filters.size() == 1) {
			securityFilter = filters.get(0);
		} else {
			securityFilter = new OrFilterBuilder(filters.toArray(new FilterBuilder[filters.size()]));
		}
		return securityFilter;
	}

	/**
	 * Setup all required aggregations on SearchRequestBuilder.
	 *
	 * @param querySettings
	 * @param srb
	 */
	protected void handleAggregationSettings(QuerySettings querySettings, final Map<String, FilterBuilder> searchFilters,
											 SearchRequestBuilder srb) {
		Map<String, Object> configuredAggregations = configService.get(ConfigService.CFGNAME_SEARCH_FULLTEXT_AGGREGATIONS_FIELDS);
		Set<String> requestedAggregations = querySettings.getAggregations();
		if (configuredAggregations != null && !configuredAggregations.isEmpty() && requestedAggregations != null
				&& !requestedAggregations.isEmpty()) {
			for (String requestedAggregation : requestedAggregations) {
				Object aggregationConfig = configuredAggregations.get(requestedAggregation);
				if (aggregationConfig != null) {
					SemiParsedAggregationConfig parsedAggregationConfig = parseAggregationType(aggregationConfig, requestedAggregation);

					// terms aggregation
					if (SemiParsedAggregationConfig.AggregationType.TERMS.toString().equals(parsedAggregationConfig.getAggregationType())) {

						int size;
						try {
							size = (int) parsedAggregationConfig.getOptionalSettings().get("size");
						} catch (Exception e) {
							throw new SettingsException("Incorrect configuration of fulltext search aggregation field '" + requestedAggregation
									+ "' in configuration document " + ConfigService.CFGNAME_SEARCH_FULLTEXT_AGGREGATIONS_FIELDS
									+ ": Invalid value of [size] field.");
						}

						// We need to apply security filter.
						Map<String, FilterBuilder> _searchFilters = searchFilters;
						if (_searchFilters != null && !_searchFilters.isEmpty()) {
							FilterBuilder securityFilter = getContentLevelSecurityFilterInternal();
							if (securityFilter != null) {
								Map<String, FilterBuilder> _searchFiltersClone = new HashMap<>();
								_searchFiltersClone.putAll(_searchFilters);
								_searchFiltersClone.put("document_level_security", securityFilter);
								_searchFilters = _searchFiltersClone;
							}
						}

						srb.addAggregation(createTermsBuilder(requestedAggregation, parsedAggregationConfig.getFieldName(), size, _searchFilters, true));
						if (_searchFilters != null && _searchFilters.containsKey(parsedAggregationConfig.getFieldName())) {
							if (parsedAggregationConfig.isFiltered()) {
								// we filter over contributors so we have to add second aggregation which provide more accurate numbers for selected
								// contributors because they can be out of normal aggregation due size limit
								srb.addAggregation(createTermsBuilder(
										requestedAggregation+"_selected", parsedAggregationConfig.getFieldName(),
												parsedAggregationConfig.getFilteredSize(), _searchFilters, false)
								);
							}
						}

					// date histogram aggregation
					} else if (SemiParsedAggregationConfig.AggregationType.DATE_HISTOGRAM.toString().equals(parsedAggregationConfig.getAggregationType())) {
						DateHistogramBuilder dhb = AggregationBuilders.dateHistogram(requestedAggregation);
						DateHistogram.Interval i = new DateHistogram.Interval(
								getDateHistogramAggregationInterval(parsedAggregationConfig.getFieldName())
						);
						dhb.field(parsedAggregationConfig.getFieldName()).interval(i);
						srb.addAggregation(dhb);
					}
				}
			}
		}
	}

	/**
	 * Return (the first) name of aggregation that is built on top of "sys_type" field.
	 * 
	 * @return (the first) name of aggregation that is built on top of "sys_type" field.
	 */
	private String getAggregationNameUsingSysTypeField() {
		Map<String, Object> configuredAggregations = configService.get(ConfigService.CFGNAME_SEARCH_FULLTEXT_AGGREGATIONS_FIELDS);
		if (configuredAggregations != null && !configuredAggregations.isEmpty()) {
			for (String aggregationName : configuredAggregations.keySet()) {
				Object aggregationConfig = configuredAggregations.get(aggregationName);
				if (aggregationConfig != null) {
					SemiParsedAggregationConfig config = parseAggregationType(aggregationConfig, aggregationName);
					if (ContentObjectFields.SYS_TYPE.equals(config.getFieldName())) {
						return aggregationName;
					}
				}
			}
		}
		return "";
	}

	/**
	 * For given set of aggregation names it returns only those using "date_histogram" aggregation type.
	 * It also returns name of their document filed.
	 * 
	 * @param aggregationNames set of aggregation names to filter
	 * @return only those aggregation names using "date_histogram" aggregation type
	 */
	private Map<String, String> filterAggregationNamesUsingDateHistogramAggregationType(Set<String> aggregationNames) {
		Map<String, String> result = new HashMap<>();
		if (aggregationNames.size() > 0) {
			Map<String, Object> configuredAggregations = configService.get(ConfigService.CFGNAME_SEARCH_FULLTEXT_AGGREGATIONS_FIELDS);
			if (configuredAggregations != null && !configuredAggregations.isEmpty()) {
				for (String aggregationName : configuredAggregations.keySet()) {
					Object aggregationConfig = configuredAggregations.get(aggregationName);
					if (aggregationConfig != null) {
						SemiParsedAggregationConfig config = parseAggregationType(aggregationConfig, aggregationName);
						if (SemiParsedAggregationConfig.AggregationType.DATE_HISTOGRAM.toString().equals(config.getAggregationType())) {
							result.put(aggregationName, config.getFieldName());
						}
					}
				}
			}
		}
		return result;
	}

	/**
	 * Get interval values for Date Histogram aggregations.
	 * 
	 * @param querySettings for search
	 * @return map with additional fields, never null
	 */
	public Map<String, String> getIntervalValuesForDateHistogramAggregations(QuerySettings querySettings) {
		Map<String, String> ret = new HashMap<>();
		Set<String> aggregations = querySettings.getAggregations();
		if (aggregations != null && !aggregations.isEmpty()) {
			Map<String, String> dateHistogramAggregations = filterAggregationNamesUsingDateHistogramAggregationType(aggregations);
			for (String aggregationName : dateHistogramAggregations.keySet()) {
				String interval = getDateHistogramAggregationInterval(dateHistogramAggregations.get(aggregationName));
				if (interval != null) {
					ret.put(aggregationName + "_interval", interval);
				}
			}
		}
		return ret;
	}

	/**
	 * Create "terms aggregation" which can be [optionally] nested in "filtered aggregation" if any filters
	 * are used and always nested in "global filter".
	 *
	 * Nesting of aggregations. First comes the global aggregation, first-level nested
	 * is filter aggregation and second-level nested is terms aggregation.
	 * <pre>
	 *   {
	 *     "aggs" : {
	 *       "aggregationName" : {
	 *         "global" : {},
	 *         // if any filters from searchFilters apply
	 *         "aggs" : {
	 *           "aggregationName_filter" : {
	 *             "filter" : { _filters_ },
	 *             // buckets
	 *             "aggs" : {
	 *               "aggregationName_buckets" : {
	 *                 "terms" : {
	 *                   "field" : ... ,
	 *                   "size" : ...
	 *                 }
	 *               }
	 *             }
	 *           }
	 *         }
	 *       }
	 *     }
	 *   }
	 * </pre>
	 *
	 * @param aggregationName top level name of the aggregation
	 * @param aggregationField index field the aggregation buckets are calculated for
	 * @param size terms field size
	 * @param searchFilters used filters
	 * @param excluding if true then filters on top of aggregationField are excluded from searchFilters
	 * @return GlobalBuilder
	 */
	protected GlobalBuilder createTermsBuilder(String aggregationName, String aggregationField, int size,
											   Map<String, FilterBuilder> searchFilters, boolean excluding) {
		FilterAggregationBuilder fab = null;
		if (searchFilters != null && !searchFilters.isEmpty()) {
			FilterBuilder[] fb = excluding ? filtersMapToArrayExcluding(searchFilters, aggregationField) :
					filtersMapToArray(searchFilters);
			if (fb != null && fb.length > 0) {
				fab = AggregationBuilders.filter(aggregationName + "_filter");
				fab.filter(new AndFilterBuilder(fb));
			}
		}
		TermsBuilder tb = AggregationBuilders.terms(aggregationName+"_buckets").field(aggregationField).size(size);
		GlobalBuilder gb = AggregationBuilders.global(aggregationName);
		if (fab != null) {
			fab.subAggregation(tb);
			gb.subAggregation(fab);
		} else {
			gb.subAggregation(tb);
		}
		return gb;
	}

	/**
	 * Return Elasticsearch interval name for Histogram aggregation.
	 *
	 * @param fieldName
	 * @return interval value or null
	 */
	protected String getDateHistogramAggregationInterval(String fieldName) {
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
	 * Handle which fields will be available in search response, including field level security (issue #150) and _source
	 * filtering (issue #184).
	 * 
	 * @param querySettings to get info about requested fields from
	 * @param srb request builder to set response content into
	 * @see <a
	 *      href="http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/search-request-fields.html">Elasticsearch
	 *      - Fields</a>
	 * @see <a
	 *      href="http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/search-request-source-filtering.html">Elasticsearch
	 *      - Source Filtering</a>
	 */
	protected void setSearchRequestFields(QuerySettings querySettings, SearchRequestBuilder srb) {

		Map<String, Object> cf = configService.get(ConfigService.CFGNAME_SEARCH_RESPONSE_FIELDS);

		List<String> fields = null;

		if (querySettings.getFields() != null) {
			fields = querySettings.getFields();
			if (fields != null && fields.contains("*")) {
				throw new BadFieldException(QuerySettings.FIELDS_KEY, "value * is invalid");
			}
		} else {
			try {
				fields = SearchUtils.getListOfStringsFromJsonMap(cf, ConfigService.CFGNAME_SEARCH_RESPONSE_FIELDS);
			} catch (ClassCastException e) {
				throw new SettingsException(ConfigService.CFGNAME_SEARCH_RESPONSE_FIELDS
						+ " configuration document is invalid.");
			}

		}

		boolean isSourceReturned = false;
		if (fields != null && !fields.isEmpty()) {
			if (cf != null) {
				@SuppressWarnings("unchecked")
				Map<String, Object> cfgFieldsPermissions = (Map<String, Object>) cf.get(CFGNAME_FIELD_VISIBLE_FOR_ROLES);
				if (cfgFieldsPermissions != null && !cfgFieldsPermissions.isEmpty()
						&& !authenticationUtilService.isUserInRole(Role.ADMIN)) {
					List<String> fieldsFiltered = new ArrayList<>();
					for (String field : fields) {
						List<String> roles = SearchUtils.getListOfStringsFromJsonMap(cfgFieldsPermissions, field);
						if (roles != null && !roles.isEmpty()) {
							if (authenticationUtilService.isUserInAnyOfRoles(false, roles)) {
								fieldsFiltered.add(field);
							}
						} else {
							fieldsFiltered.add(field);
						}
					}
					if (fieldsFiltered.isEmpty()) {
						throw new NotAuthorizedException("No permission to show any of requested content fields.");
					}
					fields = fieldsFiltered;
				}
			}

			for (String field : fields) {
				if ("_source".equals(field.toLowerCase())) {
					isSourceReturned = true;
				}
			}

			srb.addFields((fields).toArray(new String[fields.size()]));
		} else {
			isSourceReturned = true;
		}

		if (isSourceReturned && cf != null) {
			handleSearchRequestFieldsSourceExcludes(srb, cf);
		}

	}

	private void handleSearchRequestFieldsSourceExcludes(SearchRequestBuilder srb, Map<String, Object> cf) {
		@SuppressWarnings("unchecked")
		Map<String, Object> cfgExcludes = (Map<String, Object>) cf.get(CFGNAME_SOURCE_FILTERING_FOR_ROLES);
		if (cfgExcludes != null && !cfgExcludes.isEmpty() && !authenticationUtilService.isUserInRole(Role.ADMIN)) {
			List<String> excludes = new ArrayList<>();
			for (String exclude : cfgExcludes.keySet()) {
				List<String> roles = SearchUtils.getListOfStringsFromJsonMap(cfgExcludes, exclude);
				if (roles != null && !roles.isEmpty()) {
					if (!authenticationUtilService.isUserInAnyOfRoles(false, roles)) {
						excludes.add(exclude);
					}
				}
			}
			if (excludes != null && !excludes.isEmpty()) {
				srb.setFetchSource(null, excludes.toArray(new String[excludes.size()]));
			}
		}
	}

	/**
	 * @param querySettings
	 * @param srb
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
		Map<String, Object> conditions = new HashMap<>();
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

	/**
	 * Perform Search Template query.
	 *
	 * Important note:
	 * Elasticsearch Search Templates do not accept multiple values per key, see
	 * https://github.com/elasticsearch/elasticsearch/pull/8255
	 * but we are using custom build of Elasticsearch which allows for this, see
	 * https://github.com/searchisko/searchisko/issues/195
	 *
	 * @param templateName name of registered query
	 * @param templateParams parameters and values to pass into Mustache template
	 * @param filters url param filters
	 * @return search response
	 */
	public SearchResponse performSearchTemplate(final String templateName, final Map<String, Object> templateParams,
												final QuerySettings.Filters filters) {

		if (!parsedFilterConfigService.isCacheInitialized()) {
			try {
				parsedFilterConfigService.prepareFiltersForRequest(filters);
			} catch (ReflectiveOperationException e) {
				throw new ElasticsearchException("Can not prepare filters", e);
			}
		}

		SearchRequestBuilder srb = new SearchRequestBuilder(searchClientService.getClient());
		performSearchTemplateInternal(templateName, templateParams, filters, srb);
		return srb.get();
	}

	/**
	 * This method handles processing of search template.
	 *
	 * @param templateName name (id) of registered query
	 * @param templateParams parameters to pass into search template (typically obtained form all URL parameters)
	 * @param filters parsed filters instance from URL
	 * @param srb {@link org.elasticsearch.action.search.SearchRequestBuilder} instance to work upon
	 * @return {@link org.elasticsearch.action.search.SearchRequestBuilder} instance that reflects input parameters
	 */
	protected SearchRequestBuilder performSearchTemplateInternal(final String templateName, final Map<String, Object> templateParams,
																 final QuerySettings.Filters filters, SearchRequestBuilder srb) {

		// get config override values
		String overrideContentType = registeredQueryService.getOverrideSysContentTypes(templateName);
		String overrideType = registeredQueryService.getOverrideSysTypes(templateName);

		// get config default values
		String[] defaultContentTypes = registeredQueryService.getDefaultSysContentTypes(templateName);
		String[] defaultTypes = registeredQueryService.getDefaultSysTypes(templateName);

		Set<String> allQueryIndices = new LinkedHashSet<>();
		Set<String> allQueryTypes = new LinkedHashSet<>();

		boolean handled = false;

		// did client provide content type URL parameters
		if (!handled && overrideContentType != null && !overrideContentType.trim().isEmpty()) {
			List<String> requestedSysContentTypes = filters.getFilterCandidateValues(overrideContentType);
			if (requestedSysContentTypes != null && requestedSysContentTypes.size() > 0) {
				populateIndicesAndTypesForUserInRoleBasedOnContentTypes(requestedSysContentTypes, allQueryIndices, allQueryTypes);
				handled = true;
			}
		}

		// did client provide type URL parameters
		if (!handled && overrideType != null && !overrideType.trim().isEmpty()) {
			List<String> requestedSysTypes = filters.getFilterCandidateValues(overrideType);
			if (requestedSysTypes != null && requestedSysTypes.size() > 0) {
				allQueryIndices = getIndicesForUserInRoleBasedOnTypes(new HashSet<>(requestedSysTypes), false);
				handled = true;
			}
		}

        // client did not provide content type or type in URL params or they are not configured to allow for override
		if (!handled) {
			// use default content type if configured
			if (defaultContentTypes.length > 0) {
				populateIndicesAndTypesForUserInRoleBasedOnContentTypes(Arrays.asList(defaultContentTypes), allQueryIndices, allQueryTypes);
				handled = true;
			// use default type if configured
			} else if (defaultTypes.length > 0) {
				allQueryIndices = getIndicesForUserInRoleBasedOnTypes(new HashSet<>(Arrays.asList(defaultTypes)), false);
				handled = true;
			}
		}

		// if handled then we need to check if any indices or types were setup
		if (handled) {
			if (allQueryIndices.isEmpty() && allQueryTypes.isEmpty()) {
				throw new NotAuthorizedException("No content available for current user");
			}
			if (!allQueryIndices.isEmpty())
				srb.setIndices(allQueryIndices.toArray(new String[allQueryIndices.size()]));
			if (!allQueryTypes.isEmpty())
				srb.setTypes(allQueryTypes.toArray(new String[allQueryTypes.size()]));
		}

		// default to all allowed content types and types
		if (!handled) {
			// do not pass filters instance to the following method because 'content_type' and 'type'
			// params have been already processed
			setSearchRequestIndicesAndTypes(null, null, srb);
		}

		srb.setTemplateName(templateName).setTemplateType(ScriptService.ScriptType.INDEXED)
				.setTemplateParams(templateParams);

		return srb;
	}
}
