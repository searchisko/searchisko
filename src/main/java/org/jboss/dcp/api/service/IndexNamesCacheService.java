package org.jboss.dcp.api.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.ejb.Singleton;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.jboss.dcp.api.rest.SearchRestService;

/**
 * Service used to cache index names for {@link SearchRestService}.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
@Named
@ApplicationScoped
@Singleton
public class IndexNamesCacheService {

	/**
	 * Time to Live for cache [ms].
	 */
	protected long ttl = 30 * 1000;

	private Map<String, CacheItem> cache = new HashMap<String, CacheItem>();

	/**
	 * Get index names from cache for requested dcp_types.
	 * 
	 * @param dcpTypesRequested - can be null
	 * @return index names from cache or null if not there or timeouted.
	 */
	public Set<String> get(String key) {
		CacheItem ci = null;
		synchronized (cache) {
			ci = cache.get(key);
		}
		if (ci != null && ci.validTo > System.currentTimeMillis()) {
			return ci.indexNames;
		}
		return null;
	}

	/**
	 * Put index names into cache.
	 * 
	 * @param dcpTypesRequested to store index names for (can be null)
	 * @param indexNames to store into cache
	 */
	public void put(String key, Set<String> indexNames) {
		CacheItem ci = new CacheItem();
		ci.indexNames = indexNames;
		ci.validTo = System.currentTimeMillis() + ttl;
		synchronized (cache) {
			cache.put(key, ci);
		}
	}

	private static class CacheItem {
		protected long validTo;
		protected Set<String> indexNames;
	}

}
