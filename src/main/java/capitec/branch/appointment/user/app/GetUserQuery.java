package capitec.branch.appointment.user.app;

import capitec.branch.appointment.user.app.dto.EmailCommand;
import capitec.branch.appointment.user.app.dto.UsernameCommand;
import capitec.branch.appointment.user.app.port.UserQueryPort;
import capitec.branch.appointment.user.domain.User;
import capitec.branch.appointment.utils.UseCase;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.validation.annotation.Validated;

@UseCase
@Validated
@RequiredArgsConstructor
@Log4j2
public class GetUserQuery {

    private final UserQueryPort userQueryPort;

    public User execute(UsernameCommand username, String traceId) {
        return userQueryPort.getUserByUsername(username.username()).orElseThrow(() -> {
            log.error("User not found with username:{}, traceId:{}", username, traceId);
            return new NotFoundException("User is not found");
        });
    }

    public User execute(EmailCommand email, String traceId) {
        return userQueryPort.getUserByEmail(email.email()).orElseThrow(
                () -> {
                    log.error("User is not found, traceId: {}", traceId);
                    return new NotFoundException("User not found");
                }
        );
    }
}
