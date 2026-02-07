package capitec.branch.appointment.user.app;

import capitec.branch.appointment.exeption.TokenExpiredException;
import capitec.branch.appointment.user.app.dto.ChangePasswordDTO;
import capitec.branch.appointment.user.app.dto.PasswordResetDTO;
import capitec.branch.appointment.user.app.event.PasswordResetRequestEvent;
import capitec.branch.appointment.user.app.event.PasswordUpdatedEvent;
import capitec.branch.appointment.user.app.port.OtpValidationPort;
import capitec.branch.appointment.user.domain.ResetPasswordService;
import capitec.branch.appointment.user.domain.User;
import capitec.branch.appointment.user.domain.UserService;
import capitec.branch.appointment.utils.UseCase;
import capitec.branch.appointment.utils.sharekernel.ratelimit.domain.RateLimitPurpose;
import capitec.branch.appointment.utils.sharekernel.ratelimit.domain.RateLimitService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Validated
public class PasswordResetUseCase {

    private final UserService userService;
    private final ResetPasswordService resetPasswordService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final OtpValidationPort otpValidationPort;
    private final RateLimitService rateLimitService;

    @Value("${rate-limit.otp-resend.max-attempts:5}")
    private int maxAttempts;

    @Value("${rate-limit.otp-resend.window-minutes:60}")
    private int windowMinutes;

    @Value("${rate-limit.otp-resend.cooldown-seconds:60}")
    private int cooldownSeconds;

    public void passwordResetRequest(@Email @NotBlank String email, String traceId) {
        log.info("Password reset requested. email: {}, traceId: {}", email, traceId);

        User user = findUserByEmailOrThrow(email, traceId);
        publishPasswordResetRequestEvent(user, traceId);

        log.info("Password reset request event issued. username: {}, traceId: {}", user.getUsername(), traceId);
    }

    public boolean passwordChangeRequest(String username, String password, String traceId) {
        log.info("Password change requested. username: {}, traceId: {}", username, traceId);

        User user = findUserByUsernameOrThrow(username, traceId);
        boolean isVerified = userService.verifyUserCurrentPassword(username, password, traceId);

        if (isVerified) {
            publishPasswordResetRequestEvent(user, traceId);
            log.info("Password change request event issued. username: {}, traceId: {}", username, traceId);
        } else {
            log.warn("Password verification failed. username: {}, traceId: {}", username, traceId);
        }

        return isVerified;
    }

    public void passwordReset(@Valid PasswordResetDTO passwordResetDTO, String traceId) {
        log.info("Password reset initiated. email: {}, traceId: {}", passwordResetDTO.email(), traceId);

        User user = findUserByEmailOrThrow(passwordResetDTO.email(), traceId);
        validateOtpOrThrow(user, passwordResetDTO.OTP(), traceId);
        resetRateLimit(user.getUsername());
        updatePasswordOrThrow(user, passwordResetDTO.newPassword(), traceId);
        publishPasswordUpdatedEvent(user, passwordResetDTO.OTP(), traceId);

        log.info("Password reset completed. username: {}, traceId: {}", user.getUsername(), traceId);
    }

    public void passwordChange(@Valid ChangePasswordDTO changePasswordDTO, String traceId) {
        log.info("Password change initiated. username: {}, traceId: {}", changePasswordDTO.username(), traceId);

        User user = findUserByUsernameOrThrow(changePasswordDTO.username(), traceId);
        validateOtpOrThrow(user, changePasswordDTO.OTP(), traceId);
        resetRateLimit(user.getUsername());
        updatePasswordOrThrow(user, changePasswordDTO.newPassword(), traceId);
        publishPasswordUpdatedEvent(user, changePasswordDTO.OTP(), traceId);

        log.info("Password change completed. username: {}, traceId: {}", user.getUsername(), traceId);
    }


    private User findUserByEmailOrThrow(String email, String traceId) {
        return userService.getUserByEmail(email)
                .orElseThrow(() -> {
                    log.error("User not found. email: {}, traceId: {}", email, traceId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "User is not found");
                });
    }

    private User findUserByUsernameOrThrow(String username, String traceId) {
        return userService.getUserByUsername(username)
                .orElseThrow(() -> {
                    log.error("User not found. username: {}, traceId: {}", username, traceId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "User is not found");
                });
    }

    private void validateOtpOrThrow(User user, String otp, String traceId) {
        try {
            boolean isValid = otpValidationPort.validateOtp(user.getUsername(), otp, traceId);

            if (!isValid) {
                log.warn("OTP validation failed. username: {}, traceId: {}", user.getUsername(), traceId);
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid OTP code");
            }

        } catch (TokenExpiredException e) {
            handleExpiredOtp(user, traceId, e);
        } catch (ResponseStatusException e) {
            handleOtpValidationError(user.getUsername(), traceId, e);
        }
    }

    private void handleExpiredOtp(User user, String traceId, TokenExpiredException e) {
        log.warn("OTP expired. username: {}, traceId: {}", user.getUsername(), traceId);

        checkRateLimitOrThrow(user.getUsername(), traceId);
        recordResendAttempt(user.getUsername());
        publishPasswordResetRequestEvent(user, traceId);

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

    private void updatePasswordOrThrow(User user, String newPassword, String traceId) {
        user.setPassword(newPassword);
        boolean updated = resetPasswordService.passwordReset(user);

        if (!updated) {
            log.error("Failed to update password. username: {}, traceId: {}", user.getUsername(), traceId);
            throw new ResponseStatusException(
                    HttpStatus.EXPECTATION_FAILED,
                    "Failed to update password. Please try again."
            );
        }
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

    private void publishPasswordResetRequestEvent(User user, String traceId) {
        String fullName = user.getFirstname() + " " + user.getLastname();
        applicationEventPublisher.publishEvent(
                new PasswordResetRequestEvent(user.getUsername(), user.getEmail(), fullName, traceId)
        );
    }

    private void publishPasswordUpdatedEvent(User user, String otp, String traceId) {
        String fullName = user.getFirstname() + " " + user.getLastname();
        applicationEventPublisher.publishEvent(
                new PasswordUpdatedEvent(user.getUsername(), user.getEmail(), fullName, otp, traceId)
        );
    }
}
