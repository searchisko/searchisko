package org.searchisko.api.cache;

import javax.ejb.Singleton;
import javax.enterprise.context.ApplicationScoped;
import java.util.Map;

/**
 * Cache used to cache provider info inside {@link org.searchisko.api.service.ProviderService}.
 *
 * @author Lukas VLcek
 * @since 2.0.0
 */
@ApplicationScoped
@Singleton
public class RegisteredQueryCache extends ExpiringCacheBase<Map<String, Object>> {

    public RegisteredQueryCache() {
        ttl = 20L * 1000L;
    }
}