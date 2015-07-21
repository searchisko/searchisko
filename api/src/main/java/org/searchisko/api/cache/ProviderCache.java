package org.searchisko.api.cache;

import java.util.Map;

import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.enterprise.context.ApplicationScoped;

import org.searchisko.api.service.ProviderService;

/**
 * Cache used to cache provider info inside {@link ProviderService}.
 *
 * @author Vlastimil Elias (velias at redhat dot com)
 */
@ApplicationScoped
@Singleton
@Lock(LockType.READ)
public class ProviderCache extends ExpiringCacheBase<Map<String, Object>> {

	public ProviderCache() {
		ttl = 20L * 1000L;
	}

}
