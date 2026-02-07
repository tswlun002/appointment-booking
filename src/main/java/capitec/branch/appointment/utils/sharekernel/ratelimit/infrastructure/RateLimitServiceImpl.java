package capitec.branch.appointment.utils.sharekernel.ratelimit.infrastructure;

import capitec.branch.appointment.utils.sharekernel.ratelimit.domain.RateLimit;
import capitec.branch.appointment.utils.sharekernel.ratelimit.domain.RateLimitPurpose;
import capitec.branch.appointment.utils.sharekernel.ratelimit.domain.RateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static capitec.branch.appointment.utils.sharekernel.ratelimit.infrastructure.RateLimitCacheConfig.CACHE_MANAGER;
import static capitec.branch.appointment.utils.sharekernel.ratelimit.infrastructure.RateLimitCacheConfig.RATE_LIMIT_CACHE;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitServiceImpl implements RateLimitService {

    private final RateLimitRepository repository;

    private static final ExecutorService VIRTUAL_THREAD_EXECUTOR =
            Executors.newVirtualThreadPerTaskExecutor();

    @Override
    @Cacheable(value = RATE_LIMIT_CACHE, key = "#identifier + ':' + #purpose.name()",
               cacheManager = CACHE_MANAGER, unless = "#result == null")
    public Optional<RateLimit> find(String identifier, RateLimitPurpose purpose) {
        log.debug("Rate limit cache miss, fetching from DB. identifier: {}, purpose: {}", identifier, purpose);

        return repository.findByIdentifierAndPurpose(identifier, purpose.name())
                .map(RateLimitMapper::toDomain);
    }

    @Override
    @CachePut(value = RATE_LIMIT_CACHE, key = "#identifier + ':' + #purpose.name()",
              cacheManager = CACHE_MANAGER)
    public RateLimit recordAttempt(String identifier, RateLimitPurpose purpose, int windowMinutes) {
        // Find existing or create new
        RateLimit rateLimit = findFromDbOrCache(identifier, purpose)
                .map(existing -> existing.incrementAttempt(windowMinutes))
                .orElseGet(() -> RateLimit.createNew(identifier, purpose));

        // Persist to DB asynchronously using virtual thread
        persistAsync(rateLimit);

        log.debug("Rate limit attempt recorded. identifier: {}, purpose: {}, count: {}",
                identifier, purpose, rateLimit.getAttemptCount());

        return rateLimit;
    }

    @Override
    @CacheEvict(value = RATE_LIMIT_CACHE, key = "#identifier + ':' + #purpose.name()",
                cacheManager = CACHE_MANAGER)
    public void reset(String identifier, RateLimitPurpose purpose) {
        // Delete from DB asynchronously using virtual thread
        deleteAsync(identifier, purpose);
        log.info("Rate limit reset. identifier: {}, purpose: {}", identifier, purpose);
    }

    @Override
    public boolean isLimitExceeded(String identifier, RateLimitPurpose purpose, int maxAttempts, int windowMinutes) {
        return find(identifier, purpose)
                .map(rateLimit -> rateLimit.isLimitExceeded(maxAttempts, windowMinutes))
                .orElse(false);
    }

    @Override
    public boolean isCooldownPassed(String identifier, RateLimitPurpose purpose, int cooldownSeconds) {
        return find(identifier, purpose)
                .map(rateLimit -> rateLimit.isCooldownPassed(cooldownSeconds))
                .orElse(true);
    }

    /**
     * Direct DB lookup without cache (used internally to avoid cache recursion).
     */
    private Optional<RateLimit> findFromDbOrCache(String identifier, RateLimitPurpose purpose) {
        return repository.findByIdentifierAndPurpose(identifier, purpose.name())
                .map(RateLimitMapper::toDomain);
    }

    private void persistAsync(RateLimit rateLimit) {
        VIRTUAL_THREAD_EXECUTOR.submit(() -> {
            try {
                RateLimitEntity entity = repository.findByIdentifierAndPurpose(
                        rateLimit.getIdentifier(),
                        rateLimit.getPurpose().name()
                ).map(existing -> existing.withUpdatedAttempt(
                        rateLimit.getAttemptCount(),
                        rateLimit.getWindowStartAt(),
                        rateLimit.getLastAttemptAt()
                )).orElseGet(() -> RateLimitMapper.toEntity(rateLimit));

                repository.save(entity);
                log.debug("Rate limit persisted to DB. identifier: {}, purpose: {}",
                        rateLimit.getIdentifier(), rateLimit.getPurpose());
            } catch (Exception e) {
                log.error("Failed to persist rate limit. identifier: {}, purpose: {}",
                        rateLimit.getIdentifier(), rateLimit.getPurpose(), e);
            }
        });
    }

    private void deleteAsync(String identifier, RateLimitPurpose purpose) {
        VIRTUAL_THREAD_EXECUTOR.submit(() -> {
            try {
                repository.deleteByIdentifierAndPurpose(identifier, purpose.name());
                log.debug("Rate limit deleted from DB. identifier: {}, purpose: {}", identifier, purpose);
            } catch (Exception e) {
                log.error("Failed to delete rate limit. identifier: {}, purpose: {}", identifier, purpose, e);
            }
        });
    }
}
