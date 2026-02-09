package capitec.branch.appointment.authentication.app;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import capitec.branch.appointment.authentication.domain.AuthDomainException;
import capitec.branch.appointment.authentication.domain.AuthService;
import capitec.branch.appointment.authentication.domain.TokenResponse;
import capitec.branch.appointment.utils.UseCase;
import capitec.branch.appointment.utils.sharekernel.ratelimit.domain.RateLimitPurpose;
import capitec.branch.appointment.utils.sharekernel.ratelimit.domain.RateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.text.ParseException;
import java.util.Map;
import java.util.Optional;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Validated
public class LoginUseCase {

    private final AuthService authService;
    private final RateLimitService rateLimitService;

    @Value("${rate-limit.login.max-attempts:5}")
    private int maxAttempts;

    @Value("${rate-limit.login.window-minutes:60}")
    private int windowMinutes;

    public TokenResponse execute(LoginDTO loginDTO, String traceId) {
        try {
            log.info("User signing in. email: {}, traceId: {}", loginDTO.email(), traceId);

            checkRateLimitOrThrow(loginDTO.email(), traceId);

            Optional<TokenResponse> tokenOpt = authService.login(loginDTO.email(), loginDTO.password(), traceId);

            if (tokenOpt.isEmpty()) {
                handleFailedLogin(loginDTO.email(), traceId);
            }

            TokenResponse token = tokenOpt.orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
            validateUserVerified(token.getToken(), traceId);

            // Reset rate limit on successful login
            resetRateLimit(loginDTO.email());

            log.info("User signed in successfully. traceId: {}", traceId);
            return token;
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Validation failed. email: {}, traceId: {}, error: {}", loginDTO.email(), traceId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (AuthDomainException e) {
            log.error("Auth domain error. email: {}, traceId: {}, error: {}", loginDTO.email(), traceId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during login. email: {}, traceId: {}", loginDTO.email(), traceId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Login failed. Please try again later.", e);
        }
    }

    // ==================== Rate Limit Methods ====================

    private void checkRateLimitOrThrow(String email, String traceId) {
        if (rateLimitService.isLimitExceeded(email, RateLimitPurpose.LOGIN_ATTEMPT, maxAttempts, windowMinutes)) {
            long secondsUntilReset = rateLimitService.find(email, RateLimitPurpose.LOGIN_ATTEMPT)
                    .map(rl -> rl.getSecondsUntilReset(windowMinutes))
                    .orElse(0L);

            log.warn("Login rate limit exceeded. email: {}, secondsUntilReset: {}, traceId: {}",
                    email, secondsUntilReset, traceId);

            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    String.format("Too many failed login attempts. Please try again in %d minutes.", secondsUntilReset / 60 + 1)
            );
        }
    }

    private void handleFailedLogin(String email, String traceId) {
        recordFailedAttempt(email);

        int remainingAttempts = getRemainingAttempts(email);
        log.warn("Login failed. email: {}, remainingAttempts: {}, traceId: {}", email, remainingAttempts, traceId);

        if (remainingAttempts <= 0) {
            long secondsUntilReset = rateLimitService.find(email, RateLimitPurpose.LOGIN_ATTEMPT)
                    .map(rl -> rl.getSecondsUntilReset(windowMinutes))
                    .orElse((long) windowMinutes * 60);

            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    String.format("Too many failed login attempts. Account locked for %d minutes.", secondsUntilReset / 60 + 1)
            );
        }

        throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                String.format("Invalid credentials. %d attempt(s) remaining.", remainingAttempts)
        );
    }

    private void recordFailedAttempt(String email) {
        rateLimitService.recordAttempt(email, RateLimitPurpose.LOGIN_ATTEMPT, windowMinutes);
    }

    private int getRemainingAttempts(String email) {
        return rateLimitService.find(email, RateLimitPurpose.LOGIN_ATTEMPT)
                .map(rl -> maxAttempts - rl.getAttemptCount())
                .orElse(maxAttempts - 1);
    }

    private void resetRateLimit(String email) {
        rateLimitService.reset(email, RateLimitPurpose.LOGIN_ATTEMPT);
    }

    // ==================== Validation Methods ====================

    private void validateUserVerified(String token, String traceId) {
        Map<String, Object> claims = getClaims(token, traceId);
        Boolean emailVerified = (Boolean) claims.get("email_verified");

        if (emailVerified == null || !emailVerified) {
            log.warn("User email not verified. traceId: {}", traceId);
            throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED,
                    "User is not verified. Please verify your email first.");
        }
    }

    private Map<String, Object> getClaims(String token, String traceId) {
        try {
            JWTClaimsSet jwtClaimsSet = SignedJWT.parse(token).getJWTClaimsSet();
            return jwtClaimsSet.getClaims();
        } catch (ParseException e) {
            log.error("Failed to parse JWT token. traceId: {}", traceId, e);
            throw new AuthDomainException("Failed to parse authentication token", e);
        }
    }
}
