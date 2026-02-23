package capitec.branch.appointment.user.app.port;

import capitec.branch.appointment.user.domain.User;
import jakarta.validation.Valid;

/**
 * Port for user persistence operations.
 * Implemented by infrastructure (e.g., Keycloak).
 */
public interface UserPersistencePort {

    User registerUser(@Valid User user);

    boolean verifyUser(String username);

    boolean deleteUser(String username);

    void updateUserStatus(String username, Boolean enabled);

    boolean resetPassword(@Valid User user);

    boolean verifyUserCurrentPassword(String username, String password, String traceId);
}
