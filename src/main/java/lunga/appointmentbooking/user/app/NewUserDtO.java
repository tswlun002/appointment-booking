package lunga.appointmentbooking.user.app;
public record NewUserDtO(
        String email,
        String password,
        String firstname,
        String lastname
) {
}
