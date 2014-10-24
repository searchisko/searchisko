/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.common.settings.SettingsException;
import org.searchisko.api.service.ConfigService;

/**
 * Utility class that can parse aggregation and filter configurations.
 * 
 * @author Lukas Vlcek
 * @since 1.0.2
 */
public class ConfigParseUtil {

	// All filters allow to set "_cache" and "_cache_key"
	// @see http://www.elasticsearch.org/guide/en/elasticsearch/reference/1.3/query-dsl-filters.html#caching
	private static final String FILTER_CACHE = "_cache";
	private static final String FILTER_CACHE_KEY = "_cache_key";
	private static final String FILTER_CACHE_KEY_ALIAS = "_cacheKey"; // alias found in the ES codebase

	// Each filter (and query since 0.90.4) can accept "_name"
	// @see
	// http://www.elasticsearch.org/guide/en/elasticsearch/reference/1.3/search-request-named-queries-and-filters.html#search-request-named-queries-and-filters
	private static final String FILTER_NAME = "_name";

	// Searchisko custom field
	private static final String SUPPRESS_FILTERS = "_suppress";

	// terms filter
	private static final String TERMS_FILTER_TYPE = "terms";
	private static final String TERMS_FILTER_TYPE_ALIAS = "in"; // Elasticsearch alias for terms filter
	// Searchisko custom field
	private static final String TERMS_FILTER_LOWERCASE = "_lowercase";

	// range filter
	private static final String RANGE_FILTER_TYPE = "range";
	private static final String RANGE_FILTER_GTE = "gte";
	private static final String RANGE_FILTER_LTE = "lte";
	// Searchisko custom field
	private static final String RANGE_FILTER_PROCESSOR = "_processor";

	// Elasticsearch filter optional settings
	private static final List<String> OPTIONAL_SETTINGS = Arrays.asList(FILTER_CACHE, FILTER_CACHE_KEY,
			FILTER_CACHE_KEY_ALIAS, FILTER_NAME);

	/**
	 * Parse filter type.
	 * 
	 * @param filterConfig
	 * @param filterName
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static SemiParsedFilterConfig parseFilterType(final Object filterConfig, final String filterName) {
		try {
			Map<String, Object> map = (Map<String, Object>) filterConfig;
			if (map.isEmpty()
					|| !(map.containsKey(TERMS_FILTER_TYPE) || map.containsKey(TERMS_FILTER_TYPE_ALIAS) || map
							.containsKey(RANGE_FILTER_TYPE))) {
				throw new SettingsException("Invalid configuration of fulltext search filter field '" + filterName
						+ "' in configuration document " + ConfigService.CFGNAME_SEARCH_FULLTEXT_FILTER_FIELDS
						+ ": Supported type not found.");
			}

			SemiParsedFilterConfig config;

			// parse terms filter
			if ((map.containsKey(TERMS_FILTER_TYPE) || map.containsKey(TERMS_FILTER_TYPE_ALIAS))
					&& !map.containsKey(RANGE_FILTER_TYPE)) {

				config = new SemiParsedTermsFilterConfig();
				SemiParsedTermsFilterConfig conf = (SemiParsedTermsFilterConfig) config;
				conf.setFilterName(filterName);

				final String key = map.containsKey(TERMS_FILTER_TYPE) ? TERMS_FILTER_TYPE : TERMS_FILTER_TYPE_ALIAS;
				Map<String, Object> termsAggregationConfig = (Map<String, Object>) map.get(key);
				setOptionalSettings(termsAggregationConfig, config);
				Set<String> termsAggregationConfigKeys = termsAggregationConfig.keySet();
				termsAggregationConfigKeys.removeAll(OPTIONAL_SETTINGS);
				if (termsAggregationConfigKeys.size() == 1) {
					conf.setFieldName(termsAggregationConfigKeys.iterator().next());
				} else {
					throw new SettingsException("Invalid configuration of fulltext search filter field '" + filterName
							+ "' in configuration document " + ConfigService.CFGNAME_SEARCH_FULLTEXT_FILTER_FIELDS
							+ ": The configuration of [" + key + "] contains unexpected fields.");
				}

				if (map.containsKey(TERMS_FILTER_LOWERCASE)) {
					conf.setLowercase((Boolean) map.get(TERMS_FILTER_LOWERCASE));
				}

				// parse range filter
			} else if (map.containsKey(RANGE_FILTER_TYPE) && !map.containsKey(TERMS_FILTER_TYPE)) {

				config = new SemiParsedRangeFilterConfig();
				SemiParsedRangeFilterConfig conf = (SemiParsedRangeFilterConfig) config;
				conf.setFilterName(filterName);

				Map<String, Object> rangeAggregationConfig = (Map<String, Object>) map.get(RANGE_FILTER_TYPE);
				setOptionalSettings(rangeAggregationConfig, config);
				Set<String> rangeAggregationConfigKeys = rangeAggregationConfig.keySet();
				rangeAggregationConfigKeys.removeAll(OPTIONAL_SETTINGS);
				if (rangeAggregationConfigKeys.size() == 1) {
					String fieldName = rangeAggregationConfigKeys.iterator().next();
					conf.setFieldName(fieldName);
					Map<String, Object> rangeConfig = (Map<String, Object>) rangeAggregationConfig.get(fieldName);
					if (rangeConfig.containsKey(RANGE_FILTER_GTE) && !rangeConfig.containsKey(RANGE_FILTER_LTE)) {
						conf.setGte((String) rangeConfig.get(RANGE_FILTER_GTE));
					} else if (rangeConfig.containsKey(RANGE_FILTER_LTE) && !rangeConfig.containsKey(RANGE_FILTER_GTE)) {
						conf.setLte((String) rangeConfig.get(RANGE_FILTER_LTE));
					} else {
						throw new SettingsException("Invalid configuration of fulltext search filter field '" + filterName
								+ "' in configuration document " + ConfigService.CFGNAME_SEARCH_FULLTEXT_FILTER_FIELDS
								+ ": The configuration of [" + RANGE_FILTER_TYPE + ":" + fieldName + "] contains unexpected fields.");
					}
				} else {
					throw new SettingsException("Invalid configuration of fulltext search filter field '" + filterName
							+ "' in configuration document " + ConfigService.CFGNAME_SEARCH_FULLTEXT_FILTER_FIELDS
							+ ": The configuration of [" + RANGE_FILTER_TYPE + "] contains unexpected fields.");
				}
				// parse enum
				if (map.containsKey(RANGE_FILTER_PROCESSOR)) {
					String processorClassName = (String) map.get(RANGE_FILTER_PROCESSOR);
					conf.setProcessor(processorClassName);
				}
			} else {
				throw new SettingsException("Invalid configuration of fulltext search filter field '" + filterName
						+ "' in configuration document " + ConfigService.CFGNAME_SEARCH_FULLTEXT_FILTER_FIELDS
						+ ": Malformed type definition.");
			}

			// _suppress
			if (config instanceof SemiParsedFilterConfigSupportSuppressed && map.containsKey(SUPPRESS_FILTERS)) {
				Object sf = map.get(SUPPRESS_FILTERS);
				List<String> suppressed = null;
				if (sf instanceof String) {
					suppressed = new ArrayList<>();
					suppressed.add((String) sf);
				} else if (sf instanceof List) {
					suppressed = (List<String>) sf;
				}
				((SemiParsedFilterConfigSupportSuppressed) config).setSuppressed(suppressed);
			}

			return config;

		} catch (ClassCastException e) {
			throw new SettingsException("Invalid configuration of fulltext search filter field '" + filterName
					+ "' in configuration document " + ConfigService.CFGNAME_SEARCH_FULLTEXT_FILTER_FIELDS + ".");
		}
	}

	private static void setOptionalSettings(Map<String, Object> map, SemiParsedFilterConfig config) {
		if (map.containsKey(FILTER_NAME)) {
			config.setName((String) map.get(FILTER_NAME));
		}
		if (map.containsKey(FILTER_CACHE)) {
			config.setCache("true".equalsIgnoreCase((String) map.get(FILTER_CACHE)));
		}
		if (map.containsKey(FILTER_CACHE_KEY) || map.containsKey(FILTER_CACHE_KEY_ALIAS)) {
			if (config.isCache() != null && config.isCache()) {
				final String key = map.containsKey(FILTER_CACHE_KEY) ? FILTER_CACHE_KEY : FILTER_CACHE_KEY_ALIAS;
				config.setCacheKey((String) map.get(key));
			}
		}
	}

	/**
	 * Parse aggregation type.
	 * 
	 * @param aggregationConfig
	 * @param aggregationName
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static SemiParsedAggregationConfig parseAggregationType(final Object aggregationConfig, final String aggregationName) {
		try {
			Map<String, Object> map = (Map<String, Object>) aggregationConfig;
			if (map.isEmpty() || (map.size() > 1 && !map.containsKey("_filtered"))
					|| (map.size() > 2 && map.containsKey("_filtered"))) {
				throw new SettingsException("Invalid configuration of fulltext search aggregation field '" + aggregationName
						+ "' in configuration document " + ConfigService.CFGNAME_SEARCH_FULLTEXT_AGGREGATIONS_FIELDS
						+ ": Multiple aggregation type is not allowed.");
			}
			SemiParsedAggregationConfig config = new SemiParsedAggregationConfig();
			config.setAggregationName(aggregationName);
			for (String key : map.keySet()) {
				if ("_filtered".equals(key)) {
					Map<String, Object> filtered = (Map<String, Object>) map.get(key);
					config.setFilteredSize((Integer) filtered.get("size"));
					config.setFiltered(config.getFilteredSize() > 0);
				} else {
					config.setAggregationType(key);
				}
			}
			// get map one level deeper
			map = (Map<String, Object>) map.get(config.getAggregationType());
			if (!map.containsKey("field") || map.isEmpty()) {
				throw new SettingsException("Invalid configuration of fulltext search aggregation field '" + aggregationName
						+ "' in configuration document " + ConfigService.CFGNAME_SEARCH_FULLTEXT_AGGREGATIONS_FIELDS
						+ ": Missing required [field] field.");
			}
			String fieldName = (String) map.get("field");
			if (fieldName == null || fieldName.isEmpty()) {
				throw new SettingsException("Invalid configuration of fulltext search aggregation field '" + aggregationName
						+ "' in configuration document " + ConfigService.CFGNAME_SEARCH_FULLTEXT_AGGREGATIONS_FIELDS
						+ ": Invalid [field] field value.");
			}
			config.setFieldName(fieldName);
			config.setOptionalSettings(map);
			return config;
		} catch (ClassCastException e) {
			throw new SettingsException("Invalid configuration of fulltext search aggregation field '" + aggregationName
					+ "' in configuration document " + ConfigService.CFGNAME_SEARCH_FULLTEXT_AGGREGATIONS_FIELDS + ".");
		}
	}
}
