package capitec.branch.appointment.user.app;

import capitec.branch.appointment.utils.Password;
import capitec.branch.appointment.utils.Validator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PasswordResetDTO(
        @NotBlank(message = Validator.EMAIL_MESS) @Email(message = Validator.EMAIL_MESS)
        String email,
        String OTP,
        @Password String newPassword
) {
}
