package capitec.branch.appointment.utils.sharekernel.ratelimit.infrastructure;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("rate_limit")
public record RateLimitEntity(
        @Id Long id,
        String identifier,
        String purpose,
        @Column("attempt_count")
        int attemptCount,
        @Column("window_start_at")
        LocalDateTime windowStartAt,
        @Column("last_attempt_at")
        LocalDateTime lastAttemptAt
) {
    public static RateLimitEntity create(String identifier, String purpose, int attemptCount,
                                          LocalDateTime windowStartAt, LocalDateTime lastAttemptAt) {
        return new RateLimitEntity(null, identifier, purpose, attemptCount, windowStartAt, lastAttemptAt);
    }

    public RateLimitEntity withUpdatedAttempt(int attemptCount, LocalDateTime windowStartAt, LocalDateTime lastAttemptAt) {
        return new RateLimitEntity(this.id, this.identifier, this.purpose, attemptCount, windowStartAt, lastAttemptAt);
    }
}
