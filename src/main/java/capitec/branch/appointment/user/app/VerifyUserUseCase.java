package capitec.branch.appointment.user.app;

import capitec.branch.appointment.authentication.domain.TokenResponse;
import capitec.branch.appointment.exeption.OTPExpiredException;
import capitec.branch.appointment.user.app.event.UserCreatedEvent;
import capitec.branch.appointment.user.app.event.UserVerifiedEvent;
import capitec.branch.appointment.user.app.port.AuthenticationPort;
import capitec.branch.appointment.user.app.port.OtpValidationPort;
import capitec.branch.appointment.user.app.port.RoleAssignmentPort;
import capitec.branch.appointment.user.domain.USER_TYPES;
import capitec.branch.appointment.user.domain.User;
import capitec.branch.appointment.user.app.port.UserRoleService;
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
import java.util.Optional;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Validated
public class VerifyUserUseCase {

    private final UserService userService;
    private final UserRoleService userRoleService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final RoleAssignmentPort roleAssignmentPort;
    private final OtpValidationPort otpValidationPort;
    private final AuthenticationPort authenticationPort;
    private final RateLimitService rateLimitService;
    private final TransactionTemplate transactionTemplate;

    @Value("${rate-limit.otp-resend.max-attempts:5}")
    private int maxAttempts;

    @Value("${rate-limit.otp-resend.window-minutes:60}")
    private int windowMinutes;

    @Value("${rate-limit.otp-resend.cooldown-seconds:60}")
    private int cooldownSeconds;

    public Optional<TokenResponse> execute(String username, String otp, boolean isCapitecClient, String traceId) {

        User user = findUserOrThrow(username, traceId);
        validateOtp(user, otp, traceId);
        try {
            log.info("Verifying user registration. username: {}, traceId: {}", username, traceId);

            transactionTemplate.executeWithoutResult(status -> {

                verifyUserStatus(username, traceId);

            });

        } catch (Exception e) {
            log.error("User domain error. username: {}, traceId: {}, error: {}", username, traceId, e.getMessage());
            publishUserCreatedEvent(user, traceId);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "System error during verification. A new code has been sent to your email.");
        }
        try {

            assignDefaultRoles(username, isCapitecClient, traceId);
            publishUserVerifiedEvent(user, otp, traceId);
            return attemptAutoLogin(username, traceId);
        }
        catch (Exception e) {
            log.debug("Unexpected error. username: {}, traceId: {}, error: \n", username, traceId, e);
            return Optional.empty();
        }
    }

    private User findUserOrThrow(String username, String traceId) {
        return userService.getUserByUsername(username)
                .orElseThrow(() -> {
                    log.error("User not found. username: {}, traceId: {}", username, traceId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
                });
    }

    private void validateOtp(User user, String otp, String traceId) {
        try {
            boolean isValid = otpValidationPort.validateOtp(user.getUsername(), otp, traceId);

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

        // Resend OTP
        applicationEventPublisher.publishEvent(new UserCreatedEvent(
                user.getUsername(),
                user.getEmail(),
                user.getFirstname() + " " + user.getLastname(),
                traceId
        ));

        log.info("New OTP sent after expiry. username: {}, traceId: {}", user.getUsername(), traceId);

        throw new ResponseStatusException(
                HttpStatus.GONE, "OTP has expired. A new OTP has been sent to your email.", e);
    }

    private void handleOtpValidationError(String username, String traceId, ResponseStatusException e) {
        if (HttpStatus.LOCKED.equals(e.getStatusCode())) {
            log.error("OTP locked due to too many attempts. Disabling user. username: {}, traceId: {}", username, traceId);
            userService.updateUseStatus(username, false);
        }
        throw e;
    }

    private void verifyUserStatus(String username, String traceId) {
        boolean verified = userService.verifyUser(username);

        if (!verified) {
            log.error("Failed to update user verification status. username: {}, traceId: {}", username, traceId);
            throw new ResponseStatusException(
                    HttpStatus.EXPECTATION_FAILED,
                    "Temporarily failed to update verification status. Please try again."
            );
        }

        // Reset OTP resend rate limit on successful verification
        resetRateLimit(username);

        log.info("User verification status updated successfully. username: {}, traceId: {}", username, traceId);
    }

    private void assignDefaultRoles(String username, boolean isCapitecClient, String traceId) {
        String groupName = isCapitecClient ? USER_TYPES.USER_CLIENT.name() : USER_TYPES.USER_GUEST.name();

        roleAssignmentPort.getGroupId(groupName, true)
                .ifPresentOrElse(
                        groupId -> assignUserToGroup(username, groupId, traceId),
                        () -> log.warn("Default group not found. groupName: {}, username: {}, traceId: {}",
                                groupName, username, traceId)
                );
    }

    private void assignUserToGroup(String username, String groupId, String traceId) {
        boolean assigned = userRoleService.addUserToGroup(username, groupId);

        if (assigned) {
            log.info("User assigned to default group. groupId: {}, username: {}, traceId: {}", groupId, username, traceId);
        } else {
            log.warn("Failed to assign user to default group. groupId: {}, username: {}, traceId: {}", groupId, username, traceId);
        }
    }

    private void publishUserVerifiedEvent(User user, String otp, String traceId) {
        String fullName = user.getFirstname() + " " + user.getLastname();

        applicationEventPublisher.publishEvent(new UserVerifiedEvent(
                user.getUsername(),
                user.getEmail(),
                fullName,
                otp,
                traceId
        ));

        log.debug("UserVerifiedEvent published. username: {}, traceId: {}", user.getUsername(), traceId);
    }
    private void publishUserCreatedEvent(User user, String traceId) {
        String fullName = user.getFirstname() + " " + user.getLastname();
        applicationEventPublisher.publishEvent(new UserCreatedEvent(
                user.getUsername(),
                user.getEmail(),
                fullName,
                traceId
        ));
        log.debug("UserCreatedEvent published for username: {}, traceId: {}", user.getUsername(), traceId);
    }

    private Optional<TokenResponse> attemptAutoLogin(String username, String traceId) {
        try {
            TokenResponse tokenResponse = authenticationPort.impersonateUser(username, traceId);
            log.info("Auto-login successful. username: {}, traceId: {}", username, traceId);
            return Optional.ofNullable(tokenResponse);

        } catch (Exception e) {
            log.warn("Auto-login failed. User must login manually. username: {}, traceId: {}", username, traceId, e);
            return Optional.empty();
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
}
