/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.Singleton;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

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
import org.elasticsearch.search.facet.datehistogram.DateHistogramFacetBuilder;
import org.elasticsearch.search.facet.terms.TermsFacetBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.searchisko.api.ContentObjectFields;
import org.searchisko.api.cache.IndexNamesCache;
import org.searchisko.api.model.QuerySettings;
import org.searchisko.api.model.QuerySettings.Filters;
import org.searchisko.api.model.SortByValue;
import org.searchisko.api.model.TimeoutConfiguration;

/**
 * Search business logic service.
 *
 * @author Libor Krzyzanek
 * @author Vlastimil Elias (velias at redhat dot com)
 *
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
	protected IndexNamesCache indexNamesCache;

	@Inject
	protected TimeoutConfiguration timeout;

	@Inject
	protected Logger log;

	/**
	 * Perform search operation.
	 *
	 * @param querySettings to use for search
	 * @param responseUuid used for search response, we need it only to write it into statistics (so can be null)
	 * @return search response
	 */
	public SearchResponse performSearch(QuerySettings querySettings, String responseUuid, StatsRecordType statsRecordType) {
		try {
			SearchRequestBuilder srb = new SearchRequestBuilder(searchClientService.getClient());

			handleSearchIndicesAndTypes(querySettings, srb);

			QueryBuilder qb_fulltext = handleFulltextSearchSettings(querySettings);
			Map<String, FilterBuilder> searchFilters = handleCommonFiltersSettings(querySettings);
			srb.setQuery(applyCommonFilters(searchFilters, qb_fulltext));

			searchFilters.put("fulltext_query", new QueryFilterBuilder(qb_fulltext));
			handleFacetSettings(querySettings, searchFilters, srb);

			handleSortingSettings(querySettings, srb);

			handleHighlightSettings(querySettings, srb);

			handleResponseContentSettings(querySettings, srb);
			srb.setTimeout(TimeValue.timeValueSeconds(timeout.search()));

			log.log(Level.FINE, "ElasticSearch Search request: {0}", srb);

			final SearchResponse searchResponse = srb.execute().actionGet();

			statsClientService.writeStatisticsRecord(statsRecordType, responseUuid, searchResponse,
					System.currentTimeMillis(), querySettings);
			return searchResponse;
		} catch (ElasticSearchException e) {
			statsClientService.writeStatisticsRecord(statsRecordType, e, System.currentTimeMillis(), querySettings);
			throw e;
		}
	}

	/**
	 * @param querySettings
	 * @param srb
	 */
	protected void handleSearchIndicesAndTypes(QuerySettings querySettings, SearchRequestBuilder srb) {
		if (querySettings.getFilters() != null && querySettings.getFilters().getContentType() != null) {
			String type = querySettings.getFilters().getContentType();
			Map<String, Object> typeDef = providerService.findContentType(type);
			if (typeDef == null) {
				throw new IllegalArgumentException("type");
			}
            String[] queryIndices = ProviderService.extractSearchIndices(typeDef, type);
            String queryType = ProviderService.extractIndexType(typeDef, type);
			srb.setIndices(queryIndices);
			srb.setTypes(queryType);
            if (log.isLoggable(Level.FINE)) {
                log.log(Level.FINE, "Query indices: {0}", Arrays.asList(queryIndices).toString());
                log.log(Level.FINE, "Query indices type: {0}", queryType);
            }
		} else {
			List<String> sysTypesRequested = null;
			if (querySettings.getFilters() != null) {
				sysTypesRequested = querySettings.getFilters().getSysTypes();
			}
			boolean isSysTypeFacet = (querySettings.getFacets() != null && querySettings.getFacets().contains(
					getFacetNameUsingSysTypeField()));

			Set<String> indexNames = indexNamesCache.get(prepareIndexNamesCacheKey(sysTypesRequested, isSysTypeFacet));
			if (indexNames == null) {
				indexNames = new LinkedHashSet<String>();
				List<Map<String, Object>> allProviders = providerService.getAll();
				for (Map<String, Object> providerCfg : allProviders) {
					try {
						@SuppressWarnings("unchecked")
						Map<String, Map<String, Object>> types = (Map<String, Map<String, Object>>) providerCfg
								.get(ProviderService.TYPE);
						if (types != null) {
							for (String typeName : types.keySet()) {
								Map<String, Object> typeDef = types.get(typeName);
								if ((sysTypesRequested == null && !ProviderService.extractSearchAllExcluded(typeDef))
										|| (sysTypesRequested != null && ((isSysTypeFacet && !ProviderService
												.extractSearchAllExcluded(typeDef)) || sysTypesRequested.contains(ProviderService
												.extractSysType(typeDef, typeName))))) {
									indexNames.addAll(Arrays.asList(ProviderService.extractSearchIndices(typeDef, typeName)));
								}
							}
						}
					} catch (ClassCastException e) {
						throw new SettingsException("Incorrect configuration of 'type' section for sys_provider="
								+ providerCfg.get(ProviderService.NAME) + ". Contact administrators please.");
					}
				}
				indexNamesCache.put(prepareIndexNamesCacheKey(sysTypesRequested, isSysTypeFacet), indexNames);
			}
            String[] queryIndices = indexNames.toArray(new String[indexNames.size()]);
			srb.setIndices(queryIndices);
            if (log.isLoggable(Level.FINE)) {
                log.log(Level.FINE, "Query indices: {0}", Arrays.asList(queryIndices).toString());
            }
		}
	}

	/**
	 * Prepare key for indexName cache.
	 *
	 * @param sysTypesRequested to prepare key for
	 * @return key value (never null)
	 */
	protected static String prepareIndexNamesCacheKey(List<String> sysTypesRequested, boolean isSysTypFacet) {
		if (sysTypesRequested == null || sysTypesRequested.isEmpty())
			return "_all||" + isSysTypFacet;

		if (sysTypesRequested.size() == 1) {
			return sysTypesRequested.get(0) + "||" + isSysTypFacet;
		}

		TreeSet<String> ts = new TreeSet<String>(sysTypesRequested);
		StringBuilder sb = new StringBuilder();
		for (String k : ts) {
			sb.append(k).append("|");
		}
		sb.append("|").append(isSysTypFacet);
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

	protected void handleHighlightSettings(QuerySettings querySettings, SearchRequestBuilder srb) {
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
				throw new SettingsException(
						"Fulltext search highlight requested but not configured by configuration document "
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
						+ fieldName + "' parameter '" + paramName + "' in configuration document "
						+ ConfigService.CFGNAME_SEARCH_FULLTEXT_HIGHLIGHT_FIELDS + ". Contact administrators please.");
			}
		} catch (ClassCastException e) {
			throw new SettingsException("Incorrect configuration of fulltext search highlight field '" + fieldName
					+ "' in configuration document " + ConfigService.CFGNAME_SEARCH_FULLTEXT_HIGHLIGHT_FIELDS
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
			addFilter(searchFilters, ContentObjectFields.SYS_TYPE, filters.getSysTypes());
			addFilter(searchFilters, ContentObjectFields.SYS_CONTENT_PROVIDER, filters.getSysContentProvider());
			addFilter(searchFilters, ContentObjectFields.SYS_TAGS, filters.getTags());
			addFilter(searchFilters, ContentObjectFields.SYS_PROJECT, filters.getProjects());
			addFilter(searchFilters, ContentObjectFields.SYS_CONTRIBUTORS, filters.getContributors());
			if (filters.getActivityDateInterval() != null) {
				RangeFilterBuilder range = new RangeFilterBuilder(ContentObjectFields.SYS_ACTIVITY_DATES);
				range.from(DATE_TIME_FORMATTER_UTC.print(filters.getActivityDateInterval().getFromTimestamp())).includeLower(
						true);
				searchFilters.put(ContentObjectFields.SYS_ACTIVITY_DATES, range);
			} else if (filters.getActivityDateFrom() != null || filters.getActivityDateTo() != null) {
				RangeFilterBuilder range = new RangeFilterBuilder(ContentObjectFields.SYS_ACTIVITY_DATES);
				if (filters.getActivityDateFrom() != null) {
					range.from(DATE_TIME_FORMATTER_UTC.print(filters.getActivityDateFrom())).includeLower(true);
				}
				if (filters.getActivityDateTo() != null) {
					range.to(DATE_TIME_FORMATTER_UTC.print(filters.getActivityDateTo())).includeUpper(true);
				}
				searchFilters.put(ContentObjectFields.SYS_ACTIVITY_DATES, range);
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
		// TODO: Optimize! We get and parse facet configuration multiple times in this code.
		// We can cache parsed results for some time.
		Map<String, Object> configuredFacets = configService.get(ConfigService.CFGNAME_SEARCH_FULLTEXT_FACETS_FIELDS);
		Set<String> facets = querySettings.getFacets();
		if (configuredFacets != null && !configuredFacets.isEmpty() && facets != null && !facets.isEmpty()) {
			for (String queryFacetName : facets) {
				Object facetConfig = configuredFacets.get(queryFacetName);
				if (facetConfig != null) {
					SemiParsedFacetConfig config = parseFacetType(facetConfig, queryFacetName);
					if ("terms".equals(config.getFacetType())) {
						int size;
						try {
							size = (int) config.getOptionalSettings().get("size");
						} catch (Exception e) {
							throw new SettingsException("Incorrect configuration of fulltext search facet field '" + queryFacetName
									+ "' in configuration document " + ConfigService.CFGNAME_SEARCH_FULLTEXT_FACETS_FIELDS
									+ ": Invalid value of [size] field.");
						}
						srb.addFacet(createTermsFacetBuilder(queryFacetName, config.getFieldName(), size, searchFilters));
						if (searchFilters != null && searchFilters.containsKey(config.getFieldName())) {
							if (config.isFiltered()) {
								// we filter over contributors so we have to add second facet which provide numbers for these contributors
								// because they can be out of normal facet due count limitation
								TermsFacetBuilder tb = new TermsFacetBuilder(queryFacetName + "_filter")
										.field(config.getFieldName()).size(config.getFilteredSize()).global(true)
										.facetFilter(new AndFilterBuilder(getFilters(searchFilters, null)));
								srb.addFacet(tb);
							}
						}
					} else if ("date_histogram".equals(config.getFacetType())) {
						srb.addFacet(
								new DateHistogramFacetBuilder(queryFacetName)
										.field(config.getFieldName())
										.interval(selectActivityDatesHistogramInterval(querySettings)));
					}
				}
			}
		}
	}

	protected class SemiParsedFacetConfig {
		private String facetName;
		private String facetType;
		private String fieldName;
		private Map<String, Object> optionalSettings;
		private boolean filtered = false;
		private int filteredSize = 0;

		public void setFacetName(String value) { this.facetName = value; }
		public String getFacetName() { return this.facetName; }
		public void setFacetType(String value) { this.facetType = value; }
		public String getFacetType() { return this.facetType; }
		public void setFieldName(String value) { this.fieldName = value; }
		public String getFieldName() { return this.fieldName; }
		public void setOptionalSettings(Map<String, Object> object) { this.optionalSettings = object; }
		public Map<String, Object> getOptionalSettings() { return this.optionalSettings; }
		public void setFiltered(boolean value) { this.filtered = value; }
		public boolean isFiltered() { return this.filtered; }
		public void setFilteredSize(int value) { this.filteredSize = value; }
		public int getFilteredSize() { return this.filteredSize; }
	}

	/**
	 * Parse facet type.
	 * @param facetConfig
	 * @param facetName
	 * @return
	 */
	protected SemiParsedFacetConfig parseFacetType(final Object facetConfig, final String facetName) {
		try {
			Map<String, Object> map = (Map<String, Object>) facetConfig;
			if (map.isEmpty() ||
					( map.size() > 1 && !map.containsKey("_filtered") ) ||
					( map.size() > 2 && map.containsKey("_filtered"))
					) {
				throw new SettingsException("Incorrect configuration of fulltext search facet field '" + facetName
						+ "' in configuration document " + ConfigService.CFGNAME_SEARCH_FULLTEXT_FACETS_FIELDS
						+ ": Multiple facet type is not allowed.");
			}
			SemiParsedFacetConfig config = new SemiParsedFacetConfig();
			config.setFacetName(facetName);
			for (String key : map.keySet()) {
				if ("_filtered".equals(key)) {
					Map<String, Object> filtered = (Map<String, Object>) map.get(key);
					config.setFilteredSize((Integer) filtered.get("size"));
					config.setFiltered(config.getFilteredSize() > 0 ? true : false);
				} else {
					config.setFacetType(key);
				}
			}
			// get map one level deeper
			map = (Map<String, Object>) map.get(config.getFacetType());
			if (!map.containsKey("field") || map.isEmpty()) {
				throw new SettingsException("Incorrect configuration of fulltext search facet field '" + facetName
						+ "' in configuration document " + ConfigService.CFGNAME_SEARCH_FULLTEXT_FACETS_FIELDS
						+ ": Missing required [field] field.");
			}
			String fieldName = (String) map.get("field");
			if (fieldName == null || fieldName.isEmpty()) {
				throw new SettingsException("Incorrect configuration of fulltext search facet field '" + facetName
						+ "' in configuration document " + ConfigService.CFGNAME_SEARCH_FULLTEXT_FACETS_FIELDS
						+ ": Invalid [field] field value.");
			}
			config.setFieldName(fieldName);
			config.setOptionalSettings(map);
			return config;
		} catch (ClassCastException e) {
			throw new SettingsException("Incorrect configuration of fulltext search facet field '" + facetName
					+ "' in configuration document " + ConfigService.CFGNAME_SEARCH_FULLTEXT_FACETS_FIELDS
					+ ".");
		}
	}

	/**
	 * Return (the first) name of fact that is built on top of "sys_type" field.
	 * TODO: Optimize! We get and parse facet configuration multiple times in this code.
	 * We can cache parsed results for some time.
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
	 * For given set of facet names it returns only those using "date_histogram" facet type.
	 * TODO: Optimize! We get and parse facet configuration multiple times in this code.
	 * We can cache parsed results for some time.
	 *
	 * @param facetNames set of facet names to filter
	 * @return only those facets names using "date_histogram" facet type
	 */
	private Set<String> filterFacetNamesUsingDateHistogramFacetType(Set<String> facetNames) {
		Set<String> result = new LinkedHashSet<>();
		if (facetNames.size() > 0) {
			Map<String, Object> configuredFacets = configService.get(ConfigService.CFGNAME_SEARCH_FULLTEXT_FACETS_FIELDS);
			if (configuredFacets != null && !configuredFacets.isEmpty()) {
				for (String facetName : configuredFacets.keySet()) {
					Object facetConfig = configuredFacets.get(facetName);
					if (facetConfig != null) {
						SemiParsedFacetConfig config = parseFacetType(facetConfig, facetName);
						if ("date_histogram".equals(config.getFacetType())) {
							result.add(facetName);
						}
					}
				}
			}
		}
		return result;
	}

	/**
	 * Get additional fields to be added into search response.
	 *
	 * @param querySettings for search
	 * @return map with additional fields, never null
	 */
	public Map<String, String> getSearchResponseAdditionalFields(QuerySettings querySettings) {
		Map<String, String> ret = new HashMap<>();
		Set<String> facets = querySettings.getFacets();
		if (facets != null) {
			Set<String> dateHistogramFacets = filterFacetNamesUsingDateHistogramFacetType(facets);
			// TODO: hack-ish modification for configurable facets
			String interval = null;
			for (String facetName : dateHistogramFacets) {
				if (interval == null) { interval = selectActivityDatesHistogramInterval(querySettings); }
				ret.put(facetName + "_interval", interval);
			}
		}
		return ret;
	}

	protected TermsFacetBuilder createTermsFacetBuilder(String facetName, String facetField, int size,
			Map<String, FilterBuilder> searchFilters) {

		TermsFacetBuilder tb = new TermsFacetBuilder(facetName).field(facetField).size(size).global(true);
		if (searchFilters != null && !searchFilters.isEmpty()) {
			FilterBuilder[] fb = getFilters(searchFilters, facetField);
			if (fb != null && fb.length > 0)
				tb.facetFilter(new AndFilterBuilder(fb));
		}
		return tb;
	}

	@SuppressWarnings("incomplete-switch")
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
	protected void handleSortingSettings(QuerySettings querySettings, SearchRequestBuilder srb) {
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
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected void handleResponseContentSettings(QuerySettings querySettings, SearchRequestBuilder srb) {

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
							+ " configuration document is invalid. Contact administrators please.");
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
