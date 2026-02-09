package capitec.branch.appointment.sharekernel.ratelimit.domain;

import org.springframework.util.Assert;
import java.time.Duration;
import java.time.LocalDateTime;

public class RateLimit {

    private final String identifier;
    private final RateLimitPurpose purpose;
    private int attemptCount;
    private LocalDateTime windowStartAt;
    private LocalDateTime lastAttemptAt;

    private RateLimit(String identifier, RateLimitPurpose purpose, int attemptCount,
                      LocalDateTime windowStartAt, LocalDateTime lastAttemptAt) {
        Assert.hasText(identifier, "Identifier must not be blank");
        Assert.notNull(purpose, "Purpose must not be null");
        Assert.isTrue(attemptCount >= 0, "Attempt count must be non-negative");
        Assert.notNull(windowStartAt, "Window start time must not be null");
        this.identifier = identifier;
        this.purpose = purpose;
        this.attemptCount = attemptCount;
        this.windowStartAt = windowStartAt;
        this.lastAttemptAt = lastAttemptAt;
    }

    public static RateLimit createNew(String identifier, RateLimitPurpose purpose) {
        LocalDateTime now = LocalDateTime.now();
        return new RateLimit(identifier, purpose, 1, now, now);
    }

    public static RateLimit reconstitute(String identifier, RateLimitPurpose purpose,
                                          int attemptCount, LocalDateTime windowStartAt,
                                          LocalDateTime lastAttemptAt) {
        return new RateLimit(identifier, purpose, attemptCount, windowStartAt, lastAttemptAt);
    }

    public boolean isLimitExceeded(int maxAttempts, int windowMinutes) {
        return !isWindowExpired(windowMinutes) && attemptCount >= maxAttempts;
    }

    public boolean isCooldownPassed(int cooldownSeconds) {
        return lastAttemptAt == null || LocalDateTime.now().isAfter(lastAttemptAt.plusSeconds(cooldownSeconds));
    }

    public RateLimit incrementAttempt(int windowMinutes) {
        LocalDateTime now = LocalDateTime.now();
        if (isWindowExpired(windowMinutes)) {
            this.windowStartAt = now;
            this.attemptCount = 1;
        } else {
            this.attemptCount++;
        }
        this.lastAttemptAt = now;
        return this;
    }

    public RateLimit reset() {
        this.attemptCount = 0;
        this.windowStartAt = LocalDateTime.now();
        this.lastAttemptAt = null;
        return this;
    }

    public long getSecondsUntilReset(int windowMinutes) {
        LocalDateTime windowEnd = windowStartAt.plusMinutes(windowMinutes);
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(windowEnd) ? 0 : Duration.between(now, windowEnd).getSeconds();
    }

    private boolean isWindowExpired(int windowMinutes) {
        return LocalDateTime.now().isAfter(windowStartAt.plusMinutes(windowMinutes));
    }

    public String getIdentifier() { return identifier; }
    public RateLimitPurpose getPurpose() { return purpose; }
    public int getAttemptCount() { return attemptCount; }
    public LocalDateTime getWindowStartAt() { return windowStartAt; }
    public LocalDateTime getLastAttemptAt() { return lastAttemptAt; }
}
