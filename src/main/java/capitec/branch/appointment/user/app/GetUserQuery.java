package capitec.branch.appointment.user.app;

import capitec.branch.appointment.user.domain.User;
import capitec.branch.appointment.user.domain.UserService;
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

    private final UserService userService;

    public User execute(UsernameCommand username) {
        return userService.getUserByUsername(username.username()).orElseThrow(() -> {
            log.error("User not found with username:{}", username);
            return new NotFoundException("User is not found");
        });
    }

    public User execute(EmailCommand email) {
        return userService.getUserByEmail(email.email()).orElseThrow(
                () -> {
                    log.error("User is not found with email:{}", email);
                    return new NotFoundException("User not found");
                }
        );
    }
}
