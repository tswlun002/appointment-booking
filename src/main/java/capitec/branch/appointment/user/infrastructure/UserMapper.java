package capitec.branch.appointment.user.infrastructure;

import capitec.branch.appointment.user.domain.User;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Component;
import java.util.List;
/**
 * Maps Keycloak UserRepresentation to User domain object.
 * Uses the protected reconstitution constructor - no reflection needed.
 */
@Component
public class UserMapper {

    private static final String USERNAME_ATTRIBUTE = "username";

    /**
     * Maps UserRepresentation to User domain object.
     * Used when fetching existing users from Keycloak.
     * Password is null since Keycloak doesn't return it.
     */
    public User toDomain(UserRepresentation userRep) {
        if (userRep == null) {
            return null;
        }

        String username = userRep.getUsername();
        String email = userRep.getEmail();
        String firstname = userRep.getFirstName();
        String lastname = userRep.getLastName();
        boolean verified = Boolean.TRUE.equals(userRep.isEmailVerified());
        boolean enabled = Boolean.TRUE.equals(userRep.isEnabled());

        // Use the domain factory method to reconstitute user from persistence
        return User.reconstitute(
                username,
                email,
                firstname,
                lastname,
                verified,
                enabled
        );
    }

    /**
     * Maps list of UserRepresentations to User domain objects.
     */
    public List<User> toDomainList(List<UserRepresentation> userRepList) {
        if (userRepList == null) {
            return List.of();
        }

        return userRepList.stream()
                .map(this::toDomain)
                .toList();
    }

}

