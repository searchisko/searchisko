/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest.search;

import java.util.Map;

/**
 * Parsed aggregation configuration.
 *
 * @author Lukas Vlcek
 * @since 1.0.2
 */
public class SemiParsedAggregationConfig {

	/**
	 * Supported aggregation types.
	 */
	public static enum AggregationType {
		TERMS("terms"), DATE_HISTOGRAM("date_histogram");

		private String type;

		private AggregationType(String type) {
			this.type = type;
		}

		@Override
		public String toString() {
			return this.type;
		}
	}

	private String aggregationName;
	private String aggregationType;
	private String fieldName;
	private Map<String, Object> optionalSettings;
	private boolean filtered = false;
	private int filteredSize = 0;

	public void setAggregationName(String value) {
		this.aggregationName = value;
	}

	public String getAggregationName() {
		return this.aggregationName;
	}

	public void setAggregationType(String value) {
		this.aggregationType = value;
	}

	public String getAggregationType() {
		return this.aggregationType;
	}

	public void setFieldName(String value) {
		this.fieldName = value;
	}

	public String getFieldName() {
		return this.fieldName;
	}

	public void setOptionalSettings(Map<String, Object> object) {
		this.optionalSettings = object;
	}

	public Map<String, Object> getOptionalSettings() {
		return this.optionalSettings;
	}

	public void setFiltered(boolean value) {
		this.filtered = value;
	}

	public boolean isFiltered() {
		return this.filtered;
	}

	public void setFilteredSize(int value) {
		this.filteredSize = value;
	}

	public int getFilteredSize() {
		return this.filteredSize;
	}
}
