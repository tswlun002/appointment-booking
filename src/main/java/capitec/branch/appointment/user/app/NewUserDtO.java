package capitec.branch.appointment.user.app;
public record NewUserDtO(
        String email,
        String password,
        String firstname,
        String lastname
) {
}
