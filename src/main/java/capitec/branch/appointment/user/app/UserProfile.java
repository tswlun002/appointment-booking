package capitec.branch.appointment.user.app;

/**
 * Read model/DTO for user profile information.
 * Used for query responses.
 */
public record UserProfile(
        String username,
        String email,
        String fullName
) {
}

