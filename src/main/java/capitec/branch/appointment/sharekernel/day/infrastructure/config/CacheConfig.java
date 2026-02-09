package capitec.branch.appointment.sharekernel.day.infrastructure.config;

import capitec.branch.appointment.sharekernel.day.infrastructure.HolidayClientFromNagerClient;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean(HolidayClientFromNagerClient.HOLIDAY_CACHE_MANAGER)
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(HolidayClientFromNagerClient.HOLIDAY_CACHE_NAME);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(2)
                .expireAfterWrite(HolidayClientFromNagerClient.CACHE_DURATION, TimeUnit.HOURS)
                .recordStats());
        return cacheManager;
    }
}