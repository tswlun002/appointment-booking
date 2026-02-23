package capitec.branch.appointment.authentication.app;

import capitec.branch.appointment.authentication.domain.AuthDomainException;
import capitec.branch.appointment.authentication.domain.AuthService;
import capitec.branch.appointment.utils.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Validated
public class LogoutUseCase {

    private final AuthService authService;

    public void execute(String refreshToken, String traceId) {
        try {
            log.info("User logging out. traceId: {}", traceId);

            authService.logout(refreshToken, traceId);

            log.info("User logged out successfully. traceId: {}", traceId);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Validation failed. traceId: {}, error: {}", traceId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (AuthDomainException e) {
            log.error("Auth domain error. traceId: {}, error: {}", traceId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during logout. traceId: {}", traceId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Logout failed. Please try again later.", e);
        }
    }
}
