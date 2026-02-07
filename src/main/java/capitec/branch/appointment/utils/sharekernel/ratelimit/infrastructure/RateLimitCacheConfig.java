package capitec.branch.appointment.utils.sharekernel.ratelimit.infrastructure;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class RateLimitCacheConfig {

    public static final String CACHE_MANAGER = "rateLimitCacheManager";
    public static final String RATE_LIMIT_CACHE = "rateLimitCache";

    @Bean(CACHE_MANAGER)
    public CacheManager rateLimitCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(RATE_LIMIT_CACHE);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .maximumSize(10000)
                .recordStats());
        return cacheManager;
    }
}
