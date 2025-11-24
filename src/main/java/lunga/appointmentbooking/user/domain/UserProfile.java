package lunga.appointmentbooking.user.domain;

public record UserProfile(
        String username,
        String email,
        String fullName
) {
}
