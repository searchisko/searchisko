package org.searchisko.api.cache;

import java.util.Set;

import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.enterprise.context.ApplicationScoped;

import org.searchisko.api.rest.SearchRestService;

/**
 * Cache used to cache index names inside {@link SearchRestService}.
 *
 * @author Vlastimil Elias (velias at redhat dot com)
 */
@ApplicationScoped
@Singleton
@Lock(LockType.READ)
public class IndexNamesCache extends ExpiringCacheBase<Set<String>> {

	public IndexNamesCache() {
		ttl = 20L * 1000L;
	}

}
