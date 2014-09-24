/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.common.joda.time.format.DateTimeFormatter;
import org.elasticsearch.common.joda.time.format.ISODateTimeFormat;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.RangeFilterBuilder;
import org.elasticsearch.index.query.TermsFilterBuilder;
import org.searchisko.api.model.ParsableIntervalConfig;
import org.searchisko.api.model.QuerySettings;
import org.searchisko.api.rest.search.ConfigParseUtil;
import org.searchisko.api.rest.search.SemiParsedFilterConfig;
import org.searchisko.api.rest.search.SemiParsedFilterConfigSupportSuppressed;
import org.searchisko.api.rest.search.SemiParsedRangeFilterConfig;
import org.searchisko.api.rest.search.SemiParsedTermsFilterConfig;

/**
 * Service that can prepare and cache parsed configuration of filters and search filters for the request. In order to
 * prepare the cache the
 * {@link ParsedFilterConfigService#prepareFiltersForRequest(org.searchisko.api.model.QuerySettings.Filters)} method
 * must be called first.
 * 
 * @author Lukas Vlcek
 * @since 1.0.2
 */
@Named
@RequestScoped
public class ParsedFilterConfigService {

	@Inject
	protected Logger log;

	@Inject
	protected ConfigService configService;

	protected static final DateTimeFormatter DATE_TIME_FORMATTER_UTC = ISODateTimeFormat.dateTime().withZoneUTC();

	// <filter name, config>
	protected Map<String, SemiParsedFilterConfig> semiParsedFilters = null;

	// <field name, filter builder>
	protected Map<String, FilterBuilder> searchFilters = null;

	// <field name, interval range>
	protected Map<String, IntervalRange> rangeFiltersIntervals = null;

	public class IntervalRange {
		private DateTime gte;
		private DateTime lte;

		public DateTime getGte() {
			return gte;
		}

		public void setGte(DateTime value) {
			this.gte = value;
		}

		public DateTime getLte() {
			return lte;
		}

		public void setLte(DateTime value) {
			this.lte = value;
		}
	}

	/**
	 * Prepares cache of parsed filter configurations and search filters valid for actual request. This method should be
	 * called as soon as {@link QuerySettings.Filters} is available.
	 * 
	 * @param filters to use to prepare relevant parsed filter configurations into request scope
	 * @throws java.lang.ReflectiveOperationException if filter field configuration file can not be parsed correctly
	 */
	protected void prepareFiltersForRequest(QuerySettings.Filters filters) throws ReflectiveOperationException {

		semiParsedFilters = new LinkedHashMap<>();
		searchFilters = new LinkedHashMap<>();
		rangeFiltersIntervals = new LinkedHashMap<>();

		if (filters != null && !filters.getFilterCandidatesKeys().isEmpty()) {
			Map<String, Object> filtersConfig = configService.get(ConfigService.CFGNAME_SEARCH_FULLTEXT_FILTER_FIELDS);

			if (filtersConfig == null || filtersConfig.isEmpty()) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Configuration document [" + ConfigService.CFGNAME_SEARCH_FULLTEXT_FILTER_FIELDS
							+ "] not found or is empty! This might be a bug.");
				}
				return;
			}

			// collect parsed filter configurations that are relevant to filters required by client
			for (String filterCandidateKey : filters.getFilterCandidatesKeys()) {
				if (filtersConfig.containsKey(filterCandidateKey)) {
					// get filter types for filterCandidateKey and check all types are the same
					Object filterConfig = filtersConfig.get(filterCandidateKey);
					// TODO search filter configuration - cache it
					SemiParsedFilterConfig parsedFilterConfig = ConfigParseUtil.parseFilterType(filterConfig, filterCandidateKey);
					semiParsedFilters.put(filterCandidateKey, parsedFilterConfig);
				}
			}

			// iterate over all collected filters and drop those that are suppressed
			for (String filterName : semiParsedFilters.keySet().toArray(new String[semiParsedFilters.size()])) {
				// parsed filters could have been removed in the meantime so we check if it is still present
				if (semiParsedFilters.containsKey(filterName)) {
					SemiParsedFilterConfig parsedFilterConfig = semiParsedFilters.get(filterName);
					if (parsedFilterConfig instanceof SemiParsedFilterConfigSupportSuppressed) {
						List<String> suppressed = ((SemiParsedFilterConfigSupportSuppressed) parsedFilterConfig).getSuppressed();
						if (suppressed != null) {
							for (String suppress : suppressed) {
								if (semiParsedFilters.containsKey(suppress)) {
									semiParsedFilters.remove(suppress);
								}
							}
						}
					}
				}
			}

			// iterate over filters
			for (SemiParsedFilterConfig filterConfig : semiParsedFilters.values()) {
				// terms filter
				if (filterConfig instanceof SemiParsedTermsFilterConfig) {
					SemiParsedTermsFilterConfig conf = (SemiParsedTermsFilterConfig) filterConfig;
					Set<String> fn = this.getFilterNamesForDocumentField(conf.getFieldName());
					final List<String> filterValues = new ArrayList<>(filters.getFilterCandidateValues(fn));
					if (!filterValues.isEmpty()) {
						// handle <_lowercase> if specified
						if (conf.isLowercase()) {
							for (int i = 0; i < filterValues.size(); i++) {
								filterValues.set(i, filterValues.get(i).toLowerCase(Locale.ENGLISH));
							}
						}
						TermsFilterBuilder tfb = new TermsFilterBuilder(conf.getFieldName(), filterValues);

						// handle terms filter <optional_settings>
						if (conf.getName() != null) {
							tfb.filterName(conf.getName());
						}
						if (conf.isCache() != null) {
							tfb.cache(conf.isCache());
						}
						if (conf.isCache() != null && conf.isCache() && conf.getCacheKey() != null) {
							tfb.cacheKey(conf.getCacheKey());
						}
						// TODO handle tfb.execution()

						searchFilters.put(conf.getFieldName(), tfb);
					}

					// range filter
				} else if (filterConfig instanceof SemiParsedRangeFilterConfig) {
					SemiParsedRangeFilterConfig conf = (SemiParsedRangeFilterConfig) filterConfig;

					RangeFilterBuilder rfb;
					// check if there is already range filter for this document field
					if (searchFilters.containsKey(conf.getFieldName())
							&& searchFilters.get(conf.getFieldName()) instanceof RangeFilterBuilder) {
						// in this case we will be adding (or overriding) settings of existing filter
						rfb = (RangeFilterBuilder) searchFilters.get(conf.getFieldName());
					} else {
						rfb = new RangeFilterBuilder(conf.getFieldName());
					}

					final String filterValue = filters.getFirstValueForFilterCandidate(conf.getFilterName());

					ParsableIntervalConfig interval = null;
					if (filterValue != null) {

						IntervalRange intervalRange = rangeFiltersIntervals.get(conf.getFieldName());
						if (intervalRange == null) {
							intervalRange = new IntervalRange();
							rangeFiltersIntervals.put(conf.getFieldName(), intervalRange);
						}

						// handle <_processor> if specified
						if (conf.getProcessor() != null) {
							Class<?> processorClass = Class.forName(conf.getProcessor());
							if (!processorClass.isEnum()) {
								throw new RuntimeException("Class [" + conf.getProcessor() + "] is not an enum type.");
							}
							// TODO: improve ParsableIntervalConfig design to make sure this method has to be implemented
							Method m = processorClass.getMethod("parseRequestParameterValue", String.class);
							interval = (ParsableIntervalConfig) m.invoke(processorClass, filterValue);
						}
						if (conf.definesGte()) {
							if (interval != null) {
								DateTime gte = new DateTime(interval.getGteValue(System.currentTimeMillis()));
								rfb.gte(gte.toString(DATE_TIME_FORMATTER_UTC));
								intervalRange.setGte(gte);
							} else {
								rfb.gte(filterValue);
								intervalRange.setGte(DATE_TIME_FORMATTER_UTC.parseDateTime(filterValue));
							}
						} else if (conf.definesLte()) {
							if (interval != null) {
								DateTime lte = new DateTime(interval.getLteValue(System.currentTimeMillis()));
								rfb.lte(lte.toString(DATE_TIME_FORMATTER_UTC));
								intervalRange.setLte(lte);
							} else {
								rfb.lte(filterValue);
								intervalRange.setLte(DATE_TIME_FORMATTER_UTC.parseDateTime(filterValue));
							}
						}
					}

					// handle range filter <optional_settings>
					if (conf.getName() != null) {
						rfb.filterName(conf.getName());
					}
					if (conf.isCache() != null) {
						rfb.cache(conf.isCache());
					}
					if (conf.isCache() != null && conf.isCache() && conf.getCacheKey() != null) {
						rfb.cacheKey(conf.getCacheKey());
					}

					searchFilters.put(conf.getFieldName(), rfb);

				} else {
					if (log.isLoggable(Level.FINE)) {
						log.log(Level.FINE, "Unsupported SemiParsedFilterConfig type: " + filterConfig.getClass().getName());
					}
				}
			}
		}
	}

	/**
	 * Provide all valid filter configurations relevant to current request. Keys of returned map represent filter names of
	 * filter configurations for current request.
	 * 
	 * @return map of {@link org.searchisko.api.rest.search.SemiParsedFilterConfig}s for current request
	 * @throws java.lang.RuntimeException if new request filters cache hasn't been initialized yet
	 */
	public Map<String, SemiParsedFilterConfig> getFilterConfigsForRequest() {
		if (semiParsedFilters == null) {
			// this should not happen if request filters have been initialized correctly before
			throw new RuntimeException("Parsed filters not initialized yet. New request filters need to be "
					+ "initialized via prepareFiltersForRequest() method. This is probably an implementation error.");
		}
		return semiParsedFilters;
	}

	/**
	 * Provide all valid search filters relevant to current request. Keys of returned map represent document field name
	 * the search filter executes on.
	 * 
	 * @return map of {@link org.elasticsearch.index.query.FilterBuilder}s for current request
	 * @throws java.lang.RuntimeException if new request filters cache hasn't been initialized yet
	 */
	public Map<String, FilterBuilder> getSearchFiltersForRequest() {
		if (searchFilters == null) {
			// this should not happen if request filters have been initialized correctly before
			throw new RuntimeException("Search filters not initialized yet. New request filters need to be "
					+ "initialized via prepareFiltersForRequest() method. This is probably an implementation error.");
		}
		return searchFilters;
	}

	/**
	 * Provides all valid interval ranges relevant to current request. Keys of returned map represent document field name.
	 * 
	 * @return
	 * @throws java.lang.RuntimeException if new request filters cache hasn't been initialized yet
	 */
	public Map<String, IntervalRange> getRangeFiltersIntervals() {
		if (rangeFiltersIntervals == null) {
			// this should not happen if range filter intervals have been initialized correctly before
			throw new RuntimeException("Range filter intervals not initialized yet. New request filters need to be "
					+ "initialized via prepareFiltersForRequest() method. This is probably an implementation error.");
		}
		return rangeFiltersIntervals;
	}

	/**
	 * Return set of filter names configured to work on top of given field name.
	 * 
	 * @param fieldName
	 * @return Set of filter names
	 * @throws java.lang.RuntimeException if new request filters cache hasn't been initialized yet
	 */
	public Set<String> getFilterNamesForDocumentField(String fieldName) {
		Set<String> filterNames = new HashSet<>();
		for (SemiParsedFilterConfig c : this.getFilterConfigsForRequest().values()) {
			if (c.getFieldName().equals(fieldName)) {
				filterNames.add(c.getFilterName());
			}
		}
		return filterNames;
	}

	/**
	 * @return true if cache of {@link org.searchisko.api.rest.search.SemiParsedFilterConfig}s and
	 *         {@link org.elasticsearch.index.query.FilterBuilder}s has been initialized
	 */
	public boolean isCacheInitialized() {
		return (semiParsedFilters != null && searchFilters != null);
	}
}
