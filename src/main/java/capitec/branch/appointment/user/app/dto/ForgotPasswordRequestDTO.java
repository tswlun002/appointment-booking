package capitec.branch.appointment.user.app.dto;

import capitec.branch.appointment.utils.CustomerEmail;

public record ForgotPasswordRequestDTO(
        @CustomerEmail
        String email
) {
}
