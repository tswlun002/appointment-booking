package capitec.branch.appointment.authentication.app;

import capitec.branch.appointment.authentication.domain.AuthDomainException;
import capitec.branch.appointment.authentication.domain.AuthService;
import capitec.branch.appointment.authentication.domain.TokenResponse;
import capitec.branch.appointment.utils.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

/**
 * Use case for admin to impersonate a user.
 * This is typically used for debugging or customer support purposes.
 */
@Slf4j
@UseCase
@RequiredArgsConstructor
@Validated
public class ImpersonateUserUseCase {

    private final AuthService authService;

    public TokenResponse execute(String username, String traceId) {
        try {
            log.info("Admin impersonating user. username: {}, traceId: {}", username, traceId);

            TokenResponse token = authService.impersonateUser(username)
                    .orElseThrow(() -> {
                        log.error("Failed to impersonate user. username: {}, traceId: {}", username, traceId);
                        return new ResponseStatusException(HttpStatus.EXPECTATION_FAILED,
                                "Failed to impersonate user. User may not exist.");
                    });

            log.info("User impersonated successfully. username: {}, traceId: {}", username, traceId);
            return token;
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Validation failed. username: {}, traceId: {}, error: {}", username, traceId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (AuthDomainException e) {
            log.error("Auth domain error. username: {}, traceId: {}, error: {}", username, traceId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during impersonation. username: {}, traceId: {}", username, traceId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "User impersonation failed. Please try again later.", e);
        }
    }
}
