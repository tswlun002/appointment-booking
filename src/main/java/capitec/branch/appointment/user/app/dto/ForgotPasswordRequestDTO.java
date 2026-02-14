package capitec.branch.appointment.user.app.dto;

import capitec.branch.appointment.utils.ValidatorMessages;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequestDTO(
        @Email(message = ValidatorMessages.EMAIL_MESS)
        @NotBlank(message = "Email is required")
        String email
) {
}
