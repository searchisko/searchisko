/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.Singleton;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import javax.ws.rs.core.MultivaluedMap;

import org.elasticsearch.common.joda.time.format.ISODateTimeFormat;
import org.searchisko.api.model.QuerySettings;
import org.searchisko.api.model.SortByValue;
import org.searchisko.api.service.SearchService;

/**
 * Search Query parameters parser component.
 * 
 * @author Libor Krzyzanek
 * @author Lukas Vlcek
 * @author Vlastimil Elias (velias at redhat dot com)
 */
@Named
@ApplicationScoped
@Singleton
public class QuerySettingsParser {

	private final static Logger log = Logger.getLogger(QuerySettingsParser.class.getName());

	/**
	 * Parse REST parameters, validate them, sanity them, and store into {@link QuerySettings} bean.
	 * 
	 * @param params REST request params to parse
	 * @return query settings instance filled with valid search settings
	 * @throws IllegalArgumentException if some param has invalid value. Message from exception contains parameter name
	 *           and is used for error handling later!
	 */
	public QuerySettings parseUriParams(MultivaluedMap<String, String> params) throws IllegalArgumentException {

		QuerySettings settings = new QuerySettings();
		if (params == null) {
			settings.getFiltersInit();
			return settings;
		}

		// Make copy of all param keys. Remove key from this copy each time a particular param is processed.
		// The idea is to process the defined parameters first (and remove relevant keys) and then process
		// the rest, where the rest can match configured filters.
		Set<String> paramKeys = params.keySet();

		// process query
		for (String key = QuerySettings.QUERY_KEY; paramKeys.contains(key); paramKeys.remove(key)) {
			settings.setQuery(normalizeQueryString(params.getFirst(key)));
		}

		// process highlighting
		for (String key = QuerySettings.QUERY_HIGHLIGHT_KEY; paramKeys.contains(key); paramKeys.remove(key)) {
			settings.setQueryHighlight(readBooleanParam(params, key));
		}

		// process fields
		for (String key = QuerySettings.FIELDS_KEY; paramKeys.contains(key); paramKeys.remove(key)) {
			settings.setFields(normalizeListParam(params.get(key)));
		}

		// process sort
		for (String key = QuerySettings.SORT_BY_KEY; paramKeys.contains(key); paramKeys.remove(key)) {
			settings.setSortBy(SortByValue.parseRequestParameterValue(params.getFirst(key)));
		}

		// process aggregations
		for (String key = QuerySettings.AGGREGATION_KEY; paramKeys.contains(key); paramKeys.remove(key)) {
			List<String> aggregationKeys = params.get(key);
			if (aggregationKeys != null) {
				for (String key_ : aggregationKeys) {
					if (key_ != null && !key_.trim().isEmpty()) {
						settings.addAggregation(key_);
					}
				}
			}
		}

		// process from
		for (String key = QuerySettings.FROM_KEY; paramKeys.contains(key); paramKeys.remove(key)) {
			settings.setFrom(readIntegerParam(params, key));
			if (settings.getFrom() != null && settings.getFrom() < 0)
				throw new IllegalArgumentException(key);
		}

		// process size
		for (String key = QuerySettings.SIZE_KEY; paramKeys.contains(key); paramKeys.remove(key)) {
			settings.setSize(readIntegerParam(params, key));
			if (settings.getSize() != null
					&& (settings.getSize() < 0 || settings.getSize() > SearchService.RESPONSE_MAX_SIZE))
				throw new IllegalArgumentException(key);
		}

		// remaining url parameters can be all search filters
		QuerySettings.Filters filters = settings.getFiltersInit();
		for (String key : paramKeys) {
			List<String> urlValues = SearchUtils.safeList(params.get(key));
			if (urlValues != null)
				filters.acknowledgeUrlFilterCandidate(key, urlValues);
		}

		if (log.isLoggable(Level.FINE)) {
			log.fine("Requested search settings: " + settings);
		}
		return settings;
	}

	/**
	 * Sanity query in given settings. Trim it and patch wildcard if not null, else use <code>match_all:{}</code>.
	 * 
	 * @param settings to sanity query in.
	 * @throws IllegalArgumentException if settings is null
	 */
	protected void sanityQuery(QuerySettings settings) throws IllegalArgumentException {
		if (settings == null) {
			throw new IllegalArgumentException("No query settings provided!");
		}
		if (settings.getQuery() != null) {
			settings.setQuery(settings.getQuery().trim());
			settings.setQuery(patchWildcards(settings.getQuery()));
		} else {
			settings.setQuery("match_all:{}");
		}
	}

	/**
	 * Normalize search query string - trim it, return null if empty, patch wildcards.
	 * 
	 * @param query to normalize
	 * @return normalized query
	 */
	protected String normalizeQueryString(String query) {
		query = SearchUtils.trimToNull(query);
		if (query == null) {
			return null;
		}
		return patchWildcards(query);
	}

	private String patchWildcards(String q) {
		if (q != null) {
			q = q.replaceAll("\\*\\?", "*");
			q = q.replaceAll("\\?\\*", "*");
			q = q.replaceAll("\\*+", "*");
			q = q.replaceAll("\\?+", "?");
		}
		return q;
	}

	/**
	 * Read request param value as integer.
	 * 
	 * @param params to get param from
	 * @param paramKey key of param
	 * @return param value as integer or null
	 * @throws IllegalArgumentException if param value is not convertible to Integer
	 */
	protected Integer readIntegerParam(MultivaluedMap<String, String> params, String paramKey)
			throws IllegalArgumentException {
		if (params != null && params.containsKey(paramKey)) {
			try {
				String s = SearchUtils.trimToNull(params.getFirst(paramKey));
				if (s == null)
					return null;
				return new Integer(s);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException(paramKey);
			}
		}
		return null;
	}

	/**
	 * Read request param value as ISO datetime format and return it as millis.
	 * 
	 * @param params to get param from
	 * @param paramKey key of param
	 * @return param timestamp value as Long or null
	 * @throws IllegalArgumentException if datetime param value is not parsable due bad format
	 */
	protected Long readDateParam(MultivaluedMap<String, String> params, String paramKey) throws IllegalArgumentException {
		if (params != null && params.containsKey(paramKey)) {
			try {
				String s = SearchUtils.trimToNull(params.getFirst(paramKey));
				if (s == null)
					return null;
				return ISODateTimeFormat.dateTimeParser().parseMillis(s);
			} catch (Exception e) {
				throw new IllegalArgumentException(paramKey);
			}
		}
		return null;
	}

	/**
	 * Read request param value as boolean.
	 * 
	 * @param params to get param from
	 * @param paramKey key of param
	 * @return param boolean value
	 */
	protected boolean readBooleanParam(MultivaluedMap<String, String> params, String paramKey) {
		if (params != null && params.containsKey(paramKey)) {
			return Boolean.parseBoolean(SearchUtils.trimToNull(params.getFirst(paramKey)));
		}
		return false;
	}

	/**
	 * Normalize list with param values. Trim values, remove empty values, return null list if empty etc.
	 * 
	 * @param paramValue value to normalize
	 * @return normalized List param value
	 */
	protected List<String> normalizeListParam(List<String> paramValue) {
		if (paramValue == null || paramValue.isEmpty()) {
			return null;
		}
		List<String> ret = new ArrayList<String>();
		for (String s : paramValue) {
			s = SearchUtils.trimToNull(s);
			if (s != null) {
				ret.add(s);
			}
		}
		if (ret.isEmpty())
			return null;
		else
			return ret;
	}

}
