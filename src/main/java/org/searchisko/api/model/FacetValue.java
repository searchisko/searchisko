/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.model;

import org.searchisko.api.util.SearchUtils;

/**
 * Enum with names used for <code>facet</code> search request param.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 * @see QuerySettings
 */
public enum FacetValue {

	TOP_CONTRIBUTORS("top_contributors"), ACTIVITY_DATES_HISTOGRAM("activity_dates_histogram"), PER_PROJECT_COUNTS(
			"per_project_counts"), PER_SYS_TYPE_COUNTS("per_sys_type_counts"), TAG_CLOUD("tag_cloud");

	/**
	 * Value used as request parameter.
	 */
	private String value;

	private FacetValue(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return this.value;
	}

	/**
	 * Convert request parameter to enum item.
	 * 
	 * @param requestVal value from request to parse
	 * @return enum item for given request value, null if it is empty
	 * @throws IllegalArgumentException if request value is invalid
	 */
	public static FacetValue parseRequestParameterValue(String requestVal) throws IllegalArgumentException {
		requestVal = SearchUtils.trimToNull(requestVal);
		if (requestVal == null)
			return null;
		for (FacetValue n : FacetValue.values()) {
			if (n.value.equals(requestVal))
				return n;
		}
		throw new IllegalArgumentException(QuerySettings.FACETS_KEY);
	}

}
