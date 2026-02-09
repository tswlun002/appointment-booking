package capitec.branch.appointment.authentication.app;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import capitec.branch.appointment.authentication.domain.AuthDomainException;
import capitec.branch.appointment.authentication.domain.AuthService;
import capitec.branch.appointment.authentication.domain.TokenResponse;
import capitec.branch.appointment.utils.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Validated
public class RefreshTokenUseCase {

    private final AuthService authService;

    public TokenResponse execute(String refreshToken, String traceId) {
        try {
            log.info("Refreshing access token. traceId: {}", traceId);

            TokenResponse token = authService.refreshAccessToken(refreshToken, traceId)
                    .orElseThrow(() -> {
                        log.error("Failed to refresh access token. traceId: {}", traceId);
                        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token");
                    });

            validateUserVerified(token.getToken(), traceId);

            log.info("Access token refreshed successfully. traceId: {}", traceId);
            return token;
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Validation failed. traceId: {}, error: {}", traceId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (AuthDomainException e) {
            log.error("Auth domain error. traceId: {}, error: {}", traceId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during token refresh. traceId: {}", traceId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Token refresh failed. Please try again later.", e);
        }
    }

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
        } catch (Exception e) {
            log.error("Failed to parse JWT token. traceId: {}", traceId, e);
            throw new AuthDomainException("Failed to parse authentication token", e);
        }
    }
}
