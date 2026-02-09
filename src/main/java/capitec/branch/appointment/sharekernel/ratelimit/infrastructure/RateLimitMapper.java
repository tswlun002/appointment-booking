package capitec.branch.appointment.sharekernel.ratelimit.infrastructure;

import capitec.branch.appointment.sharekernel.ratelimit.domain.RateLimit;
import capitec.branch.appointment.sharekernel.ratelimit.domain.RateLimitPurpose;

public final class RateLimitMapper {

    private RateLimitMapper() {}

    public static RateLimit toDomain(RateLimitEntity entity) {
        return RateLimit.reconstitute(
                entity.identifier(),
                RateLimitPurpose.valueOf(entity.purpose()),
                entity.attemptCount(),
                entity.windowStartAt(),
                entity.lastAttemptAt()
        );
    }

    public static RateLimitEntity toEntity(RateLimit domain) {
        return RateLimitEntity.create(
                domain.getIdentifier(),
                domain.getPurpose().name(),
                domain.getAttemptCount(),
                domain.getWindowStartAt(),
                domain.getLastAttemptAt()
        );
    }
}
