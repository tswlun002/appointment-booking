package capitec.branch.appointment.staff.infrastructure.adapter;

import capitec.branch.appointment.staff.app.UserPortService;
import capitec.branch.appointment.user.app.port.UserQueryPort;
import capitec.branch.appointment.user.domain.User;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
public class UserAdapter implements UserPortService {
    private final UserQueryPort userQueryPort;

    public UserAdapter(UserQueryPort userQueryPort) {
        this.userQueryPort = userQueryPort;
    }

    @Override
    public Optional<String> execute(String username) {
        return userQueryPort.getUserByUsername(username)
                .map(User::getUsername);
    }
}
