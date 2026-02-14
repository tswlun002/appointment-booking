package capitec.branch.appointment.user.app.dto;

import capitec.branch.appointment.utils.Password;
import capitec.branch.appointment.utils.ValidatorMessages;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PasswordResetDTO(
        @NotBlank(message = ValidatorMessages.EMAIL_MESS) @Email(message = ValidatorMessages.EMAIL_MESS)
        String email,
        String OTP,
        @Password String newPassword,
        @Password String confirmPassword
) {
}
