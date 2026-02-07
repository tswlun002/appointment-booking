package capitec.branch.appointment.user.infrastructure.adapter;

import capitec.branch.appointment.user.app.port.OtpResendRateLimitPort;
import capitec.branch.appointment.utils.sharekernel.ratelimit.domain.RateLimitPurpose;
import capitec.branch.appointment.utils.sharekernel.ratelimit.domain.RateLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OtpResendRateLimitAdapter implements OtpResendRateLimitPort {

    private final RateLimitService rateLimitService;

    @Value("${rate-limit.otp-resend.max-attempts:5}")
    private int maxAttempts;

    @Value("${rate-limit.otp-resend.window-minutes:60}")
    private int windowMinutes;

    @Value("${rate-limit.otp-resend.cooldown-seconds:60}")
    private int cooldownSeconds;

    @Override
    public boolean isResendAllowed(String username) {
        return !rateLimitService.isLimitExceeded(username, RateLimitPurpose.OTP_RESEND, maxAttempts, windowMinutes);
    }

    @Override
    public boolean isCooldownPassed(String username) {
        return rateLimitService.isCooldownPassed(username, RateLimitPurpose.OTP_RESEND, cooldownSeconds);
    }

    @Override
    public void recordResendAttempt(String username) {
        rateLimitService.recordAttempt(username, RateLimitPurpose.OTP_RESEND, windowMinutes);
    }

    @Override
    public long getSecondsUntilReset(String username) {
        return rateLimitService.find(username, RateLimitPurpose.OTP_RESEND)
                .map(rateLimit -> rateLimit.getSecondsUntilReset(windowMinutes))
                .orElse(0L);
    }

    @Override
    public void reset(String username) {
        rateLimitService.reset(username, RateLimitPurpose.OTP_RESEND);
    }
}
