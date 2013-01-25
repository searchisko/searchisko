package org.jboss.dcp.api.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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
	public Set<String> get(List<String> dcpTypesRequested) {
		CacheItem ci = null;
		synchronized (cache) {
			ci = cache.get(prepareKey(dcpTypesRequested));
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
	public void put(List<String> dcpTypesRequested, Set<String> indexNames) {
		CacheItem ci = new CacheItem();
		ci.indexNames = indexNames;
		ci.validTo = System.currentTimeMillis() + ttl;
		synchronized (cache) {
			cache.put(prepareKey(dcpTypesRequested), ci);
		}
	}

	/**
	 * Prepare key for cache.
	 * 
	 * @param dcpTypesRequested to prepare key for
	 * @return key value (never null)
	 */
	protected static String prepareKey(List<String> dcpTypesRequested) {
		if (dcpTypesRequested == null || dcpTypesRequested.isEmpty())
			return "_all||";

		if (dcpTypesRequested.size() == 1) {
			return dcpTypesRequested.get(0);
		}

		TreeSet<String> ts = new TreeSet<String>(dcpTypesRequested);
		StringBuilder sb = new StringBuilder();
		for (String k : ts) {
			sb.append(k).append("|");
		}
		return sb.toString();
	}

	private static class CacheItem {
		protected long validTo;
		protected Set<String> indexNames;
	}

}
