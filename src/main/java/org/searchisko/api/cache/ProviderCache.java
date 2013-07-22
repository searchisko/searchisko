package org.searchisko.api.cache;

import java.util.Map;

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
public class ProviderCache extends TimedCacheBase<Map<String, Object>> {

	public ProviderCache() {
		ttl = 20L * 1000L;
	}

}
