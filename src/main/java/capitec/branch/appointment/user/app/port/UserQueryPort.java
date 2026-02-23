package capitec.branch.appointment.user.app.port;

import capitec.branch.appointment.user.domain.User;
import capitec.branch.appointment.utils.CustomerEmail;

import java.util.Optional;

/**
 * Port for user query operations.
 * Implemented by infrastructure (e.g., Keycloak).
 */
public interface UserQueryPort {

    Optional<User> getUserByUsername(String username);

    Optional<User> getUserByEmail(@CustomerEmail String email);

    boolean checkIfUserExists(String username);
}

