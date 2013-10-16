/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.cache;

/**
 * Interface to access simple cache with <code>String</code> keys.
 *
 * @param <T> the type of value stored in the cache
 *
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public interface ICache<T> {

	/**
	 * Get value from cache for given key.
	 *
	 * @param key to get value for
	 * @return value for key or null if not available
	 */
	public T get(String key);

	/**
	 * Put value into cache.
	 *
	 * @param key to put value for
	 * @param value to put into cache
	 */
	public void put(String key, T value);

	/**
	 * Flush cache
	 */
	public void flush();
}
