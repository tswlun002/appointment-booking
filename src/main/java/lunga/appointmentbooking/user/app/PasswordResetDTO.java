package lunga.appointmentbooking.user.app;

import lunga.appointmentbooking.utils.Password;
import lunga.appointmentbooking.utils.Validator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PasswordResetDTO(
        @NotBlank(message = Validator.EMAIL_MESS) @Email(message = Validator.EMAIL_MESS)
        String email,
        String OTP,
        @Password String newPassword
) {
}
