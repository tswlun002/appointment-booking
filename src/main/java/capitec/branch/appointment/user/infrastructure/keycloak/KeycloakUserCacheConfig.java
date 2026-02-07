package capitec.branch.appointment.user.infrastructure.keycloak;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cache configuration for Keycloak user lookups.
 * Short TTL to balance performance with data freshness.
 */
@Configuration
public class KeycloakUserCacheConfig {

    public static final String KEYCLOAK_USER_CACHE_MANAGER = "keycloakUserCacheManager";
    public static final String KEYCLOAK_USER_CACHE = "keycloakUsers";

    @Bean(KEYCLOAK_USER_CACHE_MANAGER)
    public CacheManager keycloakUserCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(KEYCLOAK_USER_CACHE);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .maximumSize(1000)
                .recordStats());
        return cacheManager;
    }
}
