package capitec.branch.appointment.user.app;

import capitec.branch.appointment.AppointmentBookingApplicationTests;
import capitec.branch.appointment.keycloak.domain.KeycloakService;
import capitec.branch.appointment.otp.domain.OTP;
import capitec.branch.appointment.otp.domain.OTPService;
import capitec.branch.appointment.sharekernel.ratelimit.domain.RateLimitPurpose;
import capitec.branch.appointment.sharekernel.ratelimit.domain.RateLimitService;
import capitec.branch.appointment.user.app.dto.NewUserDtO;
import capitec.branch.appointment.user.domain.User;
import capitec.branch.appointment.user.infrastructure.keycloak.KeycloakUserCacheConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for rate limiting on expired OTP verification.
 * Tests the scenario where a user repeatedly tries to verify with an expired OTP,
 * triggering rate limit after exceeding max attempts.
 */
class VerifyUserRateLimitTest extends AppointmentBookingApplicationTests {

    private static final String ADMIN_USERNAME = "admin";

    @Autowired
    private RegisterUserUseCase registerUserUseCase;

    @Autowired
    private VerifyUserUseCase verifyUserUseCase;

    @Autowired
    private OTPService otpService;

    @Autowired
    private RateLimitService rateLimitService;

    @Autowired
    private KeycloakService keycloakService;

    @Autowired
    @Qualifier(KeycloakUserCacheConfig.KEYCLOAK_USER_CACHE_MANAGER)
    private CacheManager cacheManager;

    @Value("${rate-limit.otp-resend.max-attempts:5}")
    private int maxAttempts;

    private User registeredUser;



    @BeforeEach
    void setUp() {
        String traceId = UUID.randomUUID().toString();

        // Register a test user
        var registerDTO = new NewUserDtO(
                "ratelimit.test@example.com",
                "RateLimit",
                "TestUser",
                "@TestPassword123",
                "@TestPassword123"
        );
        registeredUser = registerUserUseCase.execute(registerDTO, traceId);

        // Clear any existing rate limit records for this user
        rateLimitService.reset(registeredUser.getUsername(), RateLimitPurpose.OTP_RESEND);
    }

    @AfterEach
    void tearDown() {
        // Clear caches
        clearCaches();

        // Clean up rate limit records
        if (registeredUser != null) {
            rateLimitService.reset(registeredUser.getUsername(), RateLimitPurpose.OTP_RESEND);
        }

        // Clean up OTPs
        otpService.deleteAllOTP("f9ad5e5b-f4f8-42e0-bb93-26b283e6f55d");

        // Clean up test users from Keycloak
        cleanupKeycloakUsers();
    }

    @Test
    @DisplayName("Should throw TOO_MANY_REQUESTS (429) when rate limit exceeded for expired OTP verification")
    void shouldThrowTooManyRequestsWhenRateLimitExceeded() throws InterruptedException {
        // Given: A registered user with an expired OTP
        OTP otp = getLatestOtp(registeredUser.getUsername());
        expireOtp(otp);

        // When: User tries to verify with expired OTP multiple times exceeding max attempts
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            String attemptTraceId = UUID.randomUUID().toString();

            // Wait for cooldown to pass (1 second + buffer)
            Thread.sleep(1100);

            // Each attempt should throw GONE (410) as OTP is expired and new one is sent
            assertThatThrownBy(() -> verifyUserUseCase.execute(
                    registeredUser.getUsername(),
                    otp.getCode(),
                    false,
                    attemptTraceId
            ))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.GONE);
                        assertThat(rse.getReason()).contains("OTP has expired");
                    });

            // Re-expire the newly generated OTP for the next attempt
            OTP newOtp = getLatestOtp(registeredUser.getUsername());
            expireOtp(newOtp);
        }

        // Then: The next attempt should throw TOO_MANY_REQUESTS (429)
        Thread.sleep(1100); // Wait for cooldown
        String finalTraceId = UUID.randomUUID().toString();
        OTP expiredOtp = getLatestOtp(registeredUser.getUsername());
        expireOtp(expiredOtp);

        assertThatThrownBy(() -> verifyUserUseCase.execute(
                registeredUser.getUsername(),
                expiredOtp.getCode(),
                false,
                finalTraceId
        ))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                    assertThat(rse.getReason()).contains("Too many OTP requests");
                });
    }

    @Test
    @DisplayName("Should return correct error message with retry time when rate limited")
    void shouldReturnCorrectErrorMessageWhenRateLimited() throws InterruptedException {
        // Given: A user who has exceeded rate limit
        OTP otp = getLatestOtp(registeredUser.getUsername());
        expireOtp(otp);

        // Exceed rate limit
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            Thread.sleep(1100); // Wait for cooldown
            try {
                verifyUserUseCase.execute(
                        registeredUser.getUsername(),
                        otp.getCode(),
                        false,
                        UUID.randomUUID().toString()
                );
            } catch (ResponseStatusException ignored) {
                // Expected - OTP expired
            }

            // Re-expire the newly generated OTP
            otp = getLatestOtp(registeredUser.getUsername());
            expireOtp(otp);
        }

        // When: User tries again after rate limit exceeded
        Thread.sleep(1100); // Wait for cooldown
        OTP finalOtp = getLatestOtp(registeredUser.getUsername());
        expireOtp(finalOtp);

        // Then: Should get TOO_MANY_REQUESTS with retry time in message
        assertThatThrownBy(() -> verifyUserUseCase.execute(
                registeredUser.getUsername(),
                finalOtp.getCode(),
                false,
                UUID.randomUUID().toString()
        ))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                    assertThat(rse.getReason())
                            .contains("Too many OTP requests")
                            .containsPattern("try again in \\d+ minutes");
                });
    }

    @Test
    @DisplayName("Should allow verification after rate limit is reset")
    void shouldAllowVerificationAfterRateLimitReset() throws InterruptedException {
        // Given: A user who has exceeded rate limit
        OTP otp = getLatestOtp(registeredUser.getUsername());
        expireOtp(otp);

        // Exceed rate limit
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            Thread.sleep(1100); // Wait for cooldown
            try {
                verifyUserUseCase.execute(
                        registeredUser.getUsername(),
                        otp.getCode(),
                        false,
                        UUID.randomUUID().toString()
                );
            } catch (ResponseStatusException ignored) {
                // Expected
            }
            otp = getLatestOtp(registeredUser.getUsername());
            expireOtp(otp);
        }

        // Verify rate limit is in effect
        Thread.sleep(1100); // Wait for cooldown
        OTP rateLimitedOtp = getLatestOtp(registeredUser.getUsername());
        expireOtp(rateLimitedOtp);

        assertThatThrownBy(() -> verifyUserUseCase.execute(
                registeredUser.getUsername(),
                rateLimitedOtp.getCode(),
                false,
                UUID.randomUUID().toString()
        ))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                });

        // When: Rate limit is reset (simulating window expiry)
        rateLimitService.reset(registeredUser.getUsername(), RateLimitPurpose.OTP_RESEND);

        // Then: User can try verification again (will get GONE for expired, not TOO_MANY_REQUESTS)
        Thread.sleep(1100); // Wait for cooldown
        OTP newOtp = getLatestOtp(registeredUser.getUsername());
        expireOtp(newOtp);

        assertThatThrownBy(() -> verifyUserUseCase.execute(
                registeredUser.getUsername(),
                newOtp.getCode(),
                false,
                UUID.randomUUID().toString()
        ))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    // Should be GONE (expired), not TOO_MANY_REQUESTS
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.GONE);
                });
    }

    @Test
    @DisplayName("Should track rate limit attempts correctly across multiple expired OTP verifications")
    void shouldTrackRateLimitAttemptsCorrectly() throws InterruptedException {
        // Given: A registered user
        OTP otp = getLatestOtp(registeredUser.getUsername());
        expireOtp(otp);

        // When: User makes attempts less than max
        int attemptsToMake = maxAttempts - 1;
        for (int attempt = 1; attempt <= attemptsToMake; attempt++) {
            Thread.sleep(1100); // Wait for cooldown
            try {
                verifyUserUseCase.execute(
                        registeredUser.getUsername(),
                        otp.getCode(),
                        false,
                        UUID.randomUUID().toString()
                );
            } catch (ResponseStatusException ignored) {
                // Expected - OTP expired
            }
            otp = getLatestOtp(registeredUser.getUsername());
            expireOtp(otp);
        }

        // Then: User should still be able to make one more attempt (GONE, not TOO_MANY_REQUESTS)
        Thread.sleep(1100); // Wait for cooldown
        OTP nextOtp = getLatestOtp(registeredUser.getUsername());
        expireOtp(nextOtp);

        assertThatThrownBy(() -> verifyUserUseCase.execute(
                registeredUser.getUsername(),
                nextOtp.getCode(),
                false,
                UUID.randomUUID().toString()
        ))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    // Should still be GONE (one more attempt allowed)
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.GONE);
                });

        // Now the next attempt should be rate limited
        Thread.sleep(1100); // Wait for cooldown
        OTP rateLimitOtp = getLatestOtp(registeredUser.getUsername());
        expireOtp(rateLimitOtp);

        assertThatThrownBy(() -> verifyUserUseCase.execute(
                registeredUser.getUsername(),
                rateLimitOtp.getCode(),
                false,
                UUID.randomUUID().toString()
        ))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                });
    }

    @Test
    @DisplayName("Successful verification should reset rate limit counter")
    void successfulVerificationShouldResetRateLimit() throws InterruptedException {
        // Given: A user who has made some rate limited attempts
        OTP otp = getLatestOtp(registeredUser.getUsername());
        expireOtp(otp);

        // Make some attempts (but not exceeding limit)
        for (int attempt = 1; attempt <= 2; attempt++) {
            Thread.sleep(1100); // Wait for cooldown
            try {
                verifyUserUseCase.execute(
                        registeredUser.getUsername(),
                        otp.getCode(),
                        false,
                        UUID.randomUUID().toString()
                );
            } catch (ResponseStatusException ignored) {
                // Expected
            }
            otp = getLatestOtp(registeredUser.getUsername());
            expireOtp(otp);
        }

        // When: User successfully verifies with valid (non-expired) OTP
        OTP validOtp = getLatestOtp(registeredUser.getUsername());
        // Don't expire this one - use it for successful verification

        var result = verifyUserUseCase.execute(
                registeredUser.getUsername(),
                validOtp.getCode(),
                false,
                UUID.randomUUID().toString()
        );

        // Then: Verification should succeed
        assertThat(result).isPresent();

        // And rate limit should be reset (user can make max attempts again if needed)
        var rateLimit = rateLimitService.find(registeredUser.getUsername(), RateLimitPurpose.OTP_RESEND);
        assertThat(rateLimit).isEmpty();
    }

    // ==================== Helper Methods ====================

    private OTP getLatestOtp(String username) {
        return otpService.find(username).stream()
                .max(Comparator.comparing(OTP::getCreationDate))
                .orElseThrow(() -> new IllegalStateException("No OTP found for user: " + username));
    }

    private void expireOtp(OTP otp) {
        // Set creation date to past to make OTP expired (2 minutes ago, OTP expires in 1 minute)
        otp.setCreationDate(LocalDateTime.now().minusMinutes(2));
        otpService.saveOTP(otp);
    }

    private void clearCaches() {
        Cache[] caches = {
                cacheManager.getCache(KeycloakUserCacheConfig.KEYCLOAK_USER_CACHE),
                cacheManager.getCache(KeycloakUserCacheConfig.KEYCLOAK_USER_CACHE_MANAGER)
        };
        for (Cache cache : caches) {
            if (cache != null) {
                cache.clear();
            }
        }
    }

    private void cleanupKeycloakUsers() {
        UsersResource usersResource = keycloakService.getUsersResources();
        List<UserRepresentation> users = usersResource.list().stream()
                .filter(u -> !u.getUsername().equals(ADMIN_USERNAME))
                .toList();
        users.stream()
                .map(UserRepresentation::getId)
                .forEach(usersResource::delete);
    }
}
