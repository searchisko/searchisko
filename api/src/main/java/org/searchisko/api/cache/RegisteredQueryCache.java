package org.searchisko.api.cache;

import java.util.Map;

import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.enterprise.context.ApplicationScoped;

/**
 * Cache used to cache provider info inside {@link org.searchisko.api.service.ProviderService}.
 *
 * @author Lukas VLcek
 * @since 2.0.0
 */
@ApplicationScoped
@Singleton
@Lock(LockType.READ)
public class RegisteredQueryCache extends ExpiringCacheBase<Map<String, Object>> {

    public RegisteredQueryCache() {
        ttl = 20L * 1000L;
    }
}