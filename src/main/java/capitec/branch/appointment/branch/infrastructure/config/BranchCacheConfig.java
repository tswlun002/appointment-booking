package capitec.branch.appointment.branch.infrastructure.config;

import capitec.branch.appointment.branch.infrastructure.BranchDaoImpl;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BranchCacheConfig {

    @Bean("branchCacheManager")
    public CacheManager branchCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(BranchDaoImpl.CACHE_NAME);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000)
                .recordStats());
        return cacheManager;
    }
}
