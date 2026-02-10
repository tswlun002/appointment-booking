package capitec.branch.appointment.user.app;

import capitec.branch.appointment.exeption.OTPExpiredException;
import capitec.branch.appointment.otp.app.ValidateOTPService;
import capitec.branch.appointment.user.app.event.DeleteUserEvent;
import capitec.branch.appointment.user.app.event.DeleteUserRequestEvent;
import capitec.branch.appointment.user.domain.User;
import capitec.branch.appointment.user.domain.UserDomainException;
import capitec.branch.appointment.user.domain.UserService;
import capitec.branch.appointment.utils.UseCase;
import capitec.branch.appointment.sharekernel.ratelimit.domain.RateLimitPurpose;
import capitec.branch.appointment.sharekernel.ratelimit.domain.RateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Validated
public class DeleteUserUseCase {

    private final UserService userService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ValidateOTPService validateOTPService;
    private final RateLimitService rateLimitService;
    private final TransactionTemplate transactionTemplate;

    @Value("${rate-limit.otp-resend.max-attempts:5}")
    private int maxAttempts;

    @Value("${rate-limit.otp-resend.window-minutes:60}")
    private int windowMinutes;

    @Value("${rate-limit.otp-resend.cooldown-seconds:60}")
    private int cooldownSeconds;

    public boolean deleteUserRequest(String username, String password, String traceId) {
        try {
            User user = findUserOrThrow(username, traceId);
            boolean isVerified = userService.verifyUserCurrentPassword(username, password, traceId);

            if (isVerified) {
                publishDeleteUserRequestEvent(user, traceId);
            }

            return isVerified;
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Validation failed. username: {}, traceId: {}, error: {}", username, traceId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (UserDomainException e) {
            log.error("User domain error. username: {}, traceId: {}, error: {}", username, traceId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }
    }

    public boolean deleteUser(String username, String OTP, String traceId) {
        try {
            User user = findUserOrThrow(username, traceId);
            validateOtpOrThrow(user, OTP, traceId);
            resetRateLimit(username);

            // Transactional delete
            Boolean deleted = transactionTemplate.execute(status -> userService.deleteUser(username));

            if (Boolean.TRUE.equals(deleted)) {
                publishDeleteUserEvent(user, OTP, traceId);
            }

            return Boolean.TRUE.equals(deleted);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Validation failed. username: {}, traceId: {}, error: {}", username, traceId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (UserDomainException e) {
            log.error("User domain error. username: {}, traceId: {}, error: {}", username, traceId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }
    }

    // ==================== Helper Methods ====================

    private User findUserOrThrow(String username, String traceId) {
        return userService.getUserByUsername(username)
                .orElseThrow(() -> {
                    log.error("User not found. username: {}, traceId: {}", username, traceId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
                });
    }

    private void validateOtpOrThrow(User user, String otp, String traceId) {
        try {
            boolean isValid = validateOTPService.validateOTP(user.getUsername(), otp, traceId);

            if (!isValid) {
                log.warn("OTP validation failed. username: {}, traceId: {}", user.getUsername(), traceId);
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid OTP code");
            }

        } catch (OTPExpiredException e) {
            handleExpiredOtp(user, traceId, e);
        } catch (ResponseStatusException e) {
            handleOtpValidationError(user.getUsername(), traceId, e);
        }
    }

    private void handleExpiredOtp(User user, String traceId, OTPExpiredException e) {
        log.warn("OTP expired. username: {}, traceId: {}", user.getUsername(), traceId);

        checkRateLimitOrThrow(user.getUsername(), traceId);
        recordResendAttempt(user.getUsername());
        publishDeleteUserRequestEvent(user, traceId);

        log.info("New OTP sent after expiry. username: {}, traceId: {}", user.getUsername(), traceId);

        throw new ResponseStatusException(
                HttpStatus.GONE,
                "OTP has expired. A new OTP has been sent to your email.",
                e
        );
    }

    private void handleOtpValidationError(String username, String traceId, ResponseStatusException e) {
        if (HttpStatus.LOCKED.equals(e.getStatusCode())) {
            log.error("OTP locked due to too many attempts. Disabling user. username: {}, traceId: {}", username, traceId);
            userService.updateUseStatus(username, false);
        }
        throw e;
    }

    // ==================== Rate Limit Methods ====================

    private void checkRateLimitOrThrow(String username, String traceId) {
        if (!rateLimitService.isCooldownPassed(username, RateLimitPurpose.OTP_RESEND, cooldownSeconds)) {
            log.warn("OTP resend cooldown not passed. username: {}, traceId: {}", username, traceId);
            throw new ResponseStatusException(
                    HttpStatus.TOO_EARLY,
                    "Please wait few minutes before requesting a new OTP."
            );
        }

        if (rateLimitService.isLimitExceeded(username, RateLimitPurpose.OTP_RESEND, maxAttempts, windowMinutes)) {
            long secondsUntilReset = rateLimitService.find(username, RateLimitPurpose.OTP_RESEND)
                    .map(rl -> rl.getSecondsUntilReset(windowMinutes))
                    .orElse(0L);
            log.warn("OTP resend rate limit exceeded. username: {}, secondsUntilReset: {}, traceId: {}",
                    username, secondsUntilReset, traceId);
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    String.format("Too many OTP requests. Please try again in %d minutes.", secondsUntilReset / 60 + 1)
            );
        }
    }

    private void recordResendAttempt(String username) {
        rateLimitService.recordAttempt(username, RateLimitPurpose.OTP_RESEND, windowMinutes);
    }

    private void resetRateLimit(String username) {
        rateLimitService.reset(username, RateLimitPurpose.OTP_RESEND);
    }

    // ==================== Event Publishing Methods ====================

    private void publishDeleteUserRequestEvent(User user, String traceId) {
        String fullName = user.getFirstname() + " " + user.getLastname();
        applicationEventPublisher.publishEvent(
                new DeleteUserRequestEvent(user.getUsername(), user.getEmail(), fullName, traceId)
        );
        log.debug("DeleteUserRequestEvent published. username: {}, traceId: {}", user.getUsername(), traceId);
    }

    private void publishDeleteUserEvent(User user, String otp, String traceId) {
        String fullName = user.getFirstname() + " " + user.getLastname();
        applicationEventPublisher.publishEvent(
                new DeleteUserEvent(user.getUsername(), user.getEmail(), fullName, otp, traceId)
        );
        log.debug("DeleteUserEvent published. username: {}, traceId: {}", user.getUsername(), traceId);
    }
}
