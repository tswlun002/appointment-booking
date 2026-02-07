package capitec.branch.appointment.user.app.dto;


public record ChangePasswordRequestDTO(
        String username,
        String password
) {
}
