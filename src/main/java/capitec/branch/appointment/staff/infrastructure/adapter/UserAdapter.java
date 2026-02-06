package capitec.branch.appointment.staff.infrastructure.adapter;

import capitec.branch.appointment.staff.app.UserPortService;
import capitec.branch.appointment.user.domain.FetchUser;
import capitec.branch.appointment.user.domain.UserProfile;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
public class UserAdapter implements UserPortService {
    private final FetchUser userService;

    public UserAdapter(FetchUser userService) {
        this.userService = userService;

    }

    @Override
    public Optional<String> execute(String username) {
        return  userService.fetchUser(username)
                .map(UserProfile::username);
    }
}
