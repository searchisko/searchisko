/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest.search;

/**
 * Parsed filter configuration.
 *
 * @author Lukas Vlcek
 * @since 1.0.2
 */
public abstract class SemiParsedFilterConfig {
	private String filterName;
	private String fieldName;
	private String name;
	private Boolean cache;
	private String cache_key;

	public void setFilterName(String filterName) { this.filterName = filterName; }
	public String getFilterName() { return this.filterName; }

	public void setFieldName(String fieldName) { this.fieldName = fieldName; }
	public String getFieldName() { return this.fieldName; }

	/**
	 * Set value of "_name".
	 * @param name
	 */
	public void setName(String name) { this.name = name; }

	/**
	 * Get value of "_name".
	 * @return
	 */
	public String getName() { return this.name; }

	/**
	 * Set values of "_cache".
	 * @param cache
	 */
	public void setCache(Boolean cache) { this.cache = cache; }

	/**
	 * Get value of "_cache".
	 * @return
	 */
	public Boolean isCache() { return this.cache; }

	/**
	 * Set value of "_cache_key".
	 * @param cacheKey
	 */
	public void setCacheKey(String cacheKey) { this.cache_key = cacheKey; }

	/**
	 * Get value of "_cache_key".
	 * @return
	 */
	public String getCacheKey() { return this.cache_key; }

}
