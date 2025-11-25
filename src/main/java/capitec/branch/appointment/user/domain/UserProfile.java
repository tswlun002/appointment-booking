package capitec.branch.appointment.user.domain;

public record UserProfile(
        String username,
        String email,
        String fullName
) {
}
