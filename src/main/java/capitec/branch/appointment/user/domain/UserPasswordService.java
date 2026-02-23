package capitec.branch.appointment.user.domain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 * Domain service for managing user password changes.
 * This service is in the domain package so it can access User's protected setPassword method.
 */
@Slf4j
@Service
public class UserPasswordService {

    /**
     * Changes the user's password.
     * This is the only authorized way to change a user's password.
     *
     * @param user the user whose password should be changed
     * @param newPassword the new password
     * @return the user with updated password
     */
    public User changePassword(User user, String newPassword) {
        Assert.notNull(user, "User must not be null");
        Assert.hasText(newPassword, "New password must not be blank");

        user.setPassword(newPassword);

        log.debug("Password changed for user. username: {}", user.getUsername());
        return user;
    }
}
