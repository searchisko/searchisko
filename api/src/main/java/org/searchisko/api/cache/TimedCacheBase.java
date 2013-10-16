package org.searchisko.api.cache;

import java.util.HashMap;
import java.util.Map;

/**
 * Base for simple cache with timeout and <code>String</code> keys.
 *
 * @param <T> the type of value stored in the cache
 *
 * @author Vlastimil Elias (velias at redhat dot com)
 */

public abstract class TimedCacheBase<T> implements ICache<T> {

	/**
	 * Time to Live for cache [ms].
	 */
	protected long ttl = 30 * 1000;

	private Map<String, CacheItem<T>> cache = new HashMap<String, CacheItem<T>>();

	@Override
	public T get(String key) {
		CacheItem<T> ci = null;
		synchronized (cache) {
			ci = cache.get(key);
		}
		if (ci != null && ci.validTo > System.currentTimeMillis()) {
			return ci.value;
		}
		return null;
	}

	@Override
	public void put(String key, T value) {
		CacheItem<T> ci = new CacheItem<T>();
		ci.value = value;
		ci.validTo = System.currentTimeMillis() + ttl;
		synchronized (cache) {
			cache.put(key, ci);
		}
	}

	@Override
	public void flush() {
		synchronized (cache) {
			cache.clear();
		}
	}

	private static class CacheItem<T> {
		protected long validTo;
		protected T value;
	}

}
