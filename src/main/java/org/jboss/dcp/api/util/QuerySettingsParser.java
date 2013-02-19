/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.util;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.MultivaluedMap;

import org.elasticsearch.common.joda.time.format.ISODateTimeFormat;
import org.jboss.dcp.api.model.FacetValue;
import org.jboss.dcp.api.model.PastIntervalValue;
import org.jboss.dcp.api.model.QuerySettings;
import org.jboss.dcp.api.model.SortByValue;
import org.jboss.dcp.api.rest.SearchRestService;

/**
 * Query settings parser
 * 
 * @author Libor Krzyzanek
 * @author Lukas Vlcek
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class QuerySettingsParser {

	private final static Logger log = Logger.getLogger(QuerySettingsParser.class.getName());

	/**
	 * Sanity query in given settings. Trim it and patch wildchard if not null, else use <code>match_all:{}</code>.
	 * 
	 * @param settings to sanity query in.
	 * @throws IllegalArgumentException if settings is null
	 */
	public static void sanityQuery(QuerySettings settings) throws IllegalArgumentException {
		if (settings == null) {
			throw new IllegalArgumentException("No query settings provided!");
		}
		if (settings.getQuery() != null) {
			settings.setQuery(settings.getQuery().trim());
			settings.setQuery(patchWildchars(settings.getQuery()));
		} else {
			settings.setQuery("match_all:{}");
		}
	}

	/**
	 * Normalize search query string - trim it, return null if empty, patch wildchars.
	 * 
	 * @param query to normalize
	 * @return normalized query
	 */
	public static String normalizeQueryString(String query) {
		query = SearchUtils.trimmToNull(query);
		if (query == null) {
			return null;
		}
		return patchWildchars(query);
	}

	private static String patchWildchars(String q) {
		if (q != null) {
			q = q.replaceAll("\\*\\?", "*");
			q = q.replaceAll("\\?\\*", "*");
			q = q.replaceAll("\\*+", "*");
			q = q.replaceAll("\\?+", "?");
		}
		return q;
	}

	/**
	 * Parse REST parameters to standardized query settings.
	 * 
	 * @param params to parse
	 * @return query settings
	 * @throws IllegalArgumentException if some param has invalid value. Message from exception contains parameter name
	 *           and is used for error handling later!
	 */
	public static QuerySettings parseUriParams(MultivaluedMap<String, String> params) throws IllegalArgumentException {
		QuerySettings settings = new QuerySettings();
		QuerySettings.Filters filters = new QuerySettings.Filters();
		settings.setFilters(filters);
		if (params == null) {
			return settings;
		}

		settings.setQuery(normalizeQueryString(params.getFirst(QuerySettings.QUERY_KEY)));
		settings.setQueryHighlight(readBooleanParam(params, QuerySettings.QUERY_HIGHLIGHT_KEY));
		settings.setFields(normalizeListParam(params.get(QuerySettings.FIELDS_KEY)));

		filters.setContentType(SearchUtils.trimmToNull(params.getFirst(QuerySettings.Filters.CONTENT_TYPE_KEY)));
		filters.setDcpContentProvider(SearchUtils.trimmToNull(params.getFirst(QuerySettings.Filters.DCP_CONTENT_PROVIDER)));
		filters.setDcpTypes(normalizeListParam(params.get(QuerySettings.Filters.DCP_TYPES_KEY)));
		filters.setProjects(normalizeListParam(params.get(QuerySettings.Filters.PROJECTS_KEY)));
		filters.setTags(normalizeListParam(params.get(QuerySettings.Filters.TAGS_KEY)));
		filters.setContributors(normalizeListParam(params.get(QuerySettings.Filters.CONTRIBUTORS_KEY)));
		filters.setActivityDateInterval(PastIntervalValue.parseRequestParameterValue(params
				.getFirst(QuerySettings.Filters.ACTIVITY_DATE_INTERVAL_KEY)));
		filters.setActivityDateFrom(readDateParam(params, QuerySettings.Filters.ACTIVITY_DATE_FROM_KEY));
		filters.setActivityDateTo(readDateParam(params, QuerySettings.Filters.ACTIVITY_DATE_TO_KEY));

		filters.setFrom(readIntegerParam(params, QuerySettings.Filters.FROM_KEY));
		if (filters.getFrom() != null && filters.getFrom() < 0)
			throw new IllegalArgumentException(QuerySettings.Filters.FROM_KEY);
		filters.setSize(readIntegerParam(params, QuerySettings.Filters.SIZE_KEY));
		if (filters.getSize() != null && (filters.getSize() < 0 || filters.getSize() > SearchRestService.RESPONSE_MAX_SIZE))
			throw new IllegalArgumentException(QuerySettings.Filters.SIZE_KEY);

		settings.setSortBy(SortByValue.parseRequestParameterValue(params.getFirst(QuerySettings.SORT_BY_KEY)));

		if (params.get(QuerySettings.FACETS_KEY) != null) {
			for (String fpv : params.get(QuerySettings.FACETS_KEY)) {
				settings.addFacet(FacetValue.parseRequestParameterValue(fpv));
			}
		}

		if (log.isLoggable(Level.FINE)) {
			log.fine("Requested search settings: " + settings);
		}
		return settings;
	}

	/**
	 * Read request param value as integer.
	 * 
	 * @param params to get param from
	 * @param paramKey key of param
	 * @return param value as integer or null
	 * @throws IllegalArgumentException if param value is not convertible to Integer
	 */
	protected static Integer readIntegerParam(MultivaluedMap<String, String> params, String paramKey)
			throws IllegalArgumentException {
		if (params != null && params.containsKey(paramKey)) {
			try {
				String s = SearchUtils.trimmToNull(params.getFirst(paramKey));
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
	 * @throws IllegalArgumentException if datetime param value is not parseable due bad format
	 */
	protected static Long readDateParam(MultivaluedMap<String, String> params, String paramKey)
			throws IllegalArgumentException {
		if (params != null && params.containsKey(paramKey)) {
			try {
				String s = SearchUtils.trimmToNull(params.getFirst(paramKey));
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
	protected static boolean readBooleanParam(MultivaluedMap<String, String> params, String paramKey) {
		if (params != null && params.containsKey(paramKey)) {
			return Boolean.parseBoolean(SearchUtils.trimmToNull(params.getFirst(paramKey)));
		}
		return false;
	}

	/**
	 * Normalize list with param values. Trim values, remove empty values, return null list if empty etc.
	 * 
	 * @param paramValue value to normalize
	 * @return normalized List param value
	 */
	protected static List<String> normalizeListParam(List<String> paramValue) {
		if (paramValue == null || paramValue.isEmpty()) {
			return null;
		}
		List<String> ret = new ArrayList<String>();
		for (String s : paramValue) {
			s = SearchUtils.trimmToNull(s);
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
