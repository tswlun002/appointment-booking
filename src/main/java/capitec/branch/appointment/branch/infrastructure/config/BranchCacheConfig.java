package capitec.branch.appointment.branch.infrastructure.config;

import capitec.branch.appointment.branch.infrastructure.BranchDaoImpl;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
@Configuration
public class BranchCacheConfig {

    @Bean(BranchDaoImpl.CACHE_MANAGER_NAME)
    @Primary
    public CacheManager branchCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(BranchDaoImpl.CACHE_NAME);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000)
                .recordStats());
        return cacheManager;
    }
}
