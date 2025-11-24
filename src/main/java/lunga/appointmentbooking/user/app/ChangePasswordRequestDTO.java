package lunga.appointmentbooking.user.app;


public record ChangePasswordRequestDTO(
        String username,
        String password
) {
}
