package org.jboss.dcp.api.cache;

import java.util.Map;

import javax.ejb.Singleton;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.jboss.dcp.api.service.ProviderService;

/**
 * Cache used to cache provider info inside {@link ProviderService}.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
@Named
@ApplicationScoped
@Singleton
public class ProviderCache extends TimedCacheBase<Map<String, Object>> {

	public ProviderCache() {
		ttl = 20L * 1000L;
	}

}
