package org.jboss.dcp.api.cache;

import java.util.Set;

import javax.ejb.Singleton;
import javax.enterprise.context.ApplicationScoped;

import org.jboss.dcp.api.rest.SearchRestService;

/**
 * Cache used to cache index names inside {@link SearchRestService}.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
@ApplicationScoped
@Singleton
public class IndexNamesCache extends TimedCacheBase<Set<String>> {

	public IndexNamesCache() {
		ttl = 20L * 1000L;
	}

}
