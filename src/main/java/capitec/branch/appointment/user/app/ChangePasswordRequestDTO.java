package capitec.branch.appointment.user.app;


public record ChangePasswordRequestDTO(
        String username,
        String password
) {
}
