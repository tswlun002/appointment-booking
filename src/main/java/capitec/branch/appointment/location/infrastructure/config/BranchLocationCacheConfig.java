package capitec.branch.appointment.location.infrastructure.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

import static capitec.branch.appointment.location.infrastructure.api.CapitecBranchLocationFetcher.BRANCH_LOCATIONS_BY_AREA_CACHE;
import static capitec.branch.appointment.location.infrastructure.api.CapitecBranchLocationFetcher.BRANCH_LOCATIONS_BY_COORDINATES_CACHE;

@Configuration
@EnableCaching
public class BranchLocationCacheConfig {

    @Bean("branchLocationCacheManager")
    public CacheManager branchLocationCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                BRANCH_LOCATIONS_BY_COORDINATES_CACHE,
                BRANCH_LOCATIONS_BY_AREA_CACHE
        );
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(24, TimeUnit.HOURS)
                .maximumSize(1000)
                .recordStats());
        return cacheManager;
    }
}

