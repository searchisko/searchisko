/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

/**
 * Types of statistics records written over {@link StatsClientService}.
 *
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public enum StatsRecordType {

	SEARCH,
	FEED,
	SEARCH_HIT_USED,

	/**
	 * Statistic for Audit log
	 */
	AUDIT;

	/**
	 * Get value representing this type to be stored into search index.
	 *
	 * @return value for index
	 */
	public String getSearchIndexedValue() {
		return name().toLowerCase();
	}

	/**
	 * Return name of Elasticsearch index used to store this statistics type.
	 *
	 * @return name of Elasticsearch index used to store this statistics type.
	 */
	public String getSearchIndexName() {
		return "stats_api_" + getSearchIndexedValue();
	}

	/**
	 * Get name of Elasticsearch type used to store this statistics type.
	 *
	 * @return name of Elasticsearch type used to store this statistics type.
	 */
	public String getSearchIndexType() {
		return getSearchIndexedValue();
	}

}
