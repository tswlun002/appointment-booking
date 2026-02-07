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
public class VerifyPasswordUseCase {

    private final AuthService authService;

    public boolean execute(String username, String password, String traceId) {
        try {
            log.info("Verifying user password. username: {}, traceId: {}", username, traceId);

            boolean verified = authService.verifyCurrentPassword(username, password, traceId);

            if (verified) {
                log.info("Password verified successfully. username: {}, traceId: {}", username, traceId);
            } else {
                log.warn("Password verification failed. username: {}, traceId: {}", username, traceId);
            }

            return verified;
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Validation failed. username: {}, traceId: {}, error: {}", username, traceId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (AuthDomainException e) {
            log.error("Auth domain error. username: {}, traceId: {}, error: {}", username, traceId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }
    }
}
