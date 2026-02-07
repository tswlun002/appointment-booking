package capitec.branch.appointment.utils.sharekernel.ratelimit.domain;

import java.util.Optional;

/**
 * Domain service interface for rate limiting operations.
 */
public interface RateLimitService {

    Optional<RateLimit> find(String identifier, RateLimitPurpose purpose);

    RateLimit recordAttempt(String identifier, RateLimitPurpose purpose, int windowMinutes);

    void reset(String identifier, RateLimitPurpose purpose);

    boolean isLimitExceeded(String identifier, RateLimitPurpose purpose, int maxAttempts, int windowMinutes);

    boolean isCooldownPassed(String identifier, RateLimitPurpose purpose, int cooldownSeconds);
}
