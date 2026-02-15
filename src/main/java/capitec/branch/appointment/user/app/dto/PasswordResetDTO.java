package capitec.branch.appointment.user.app.dto;

import capitec.branch.appointment.utils.CustomerEmail;
import capitec.branch.appointment.utils.Password;

public record PasswordResetDTO(
        @CustomerEmail
        String email,
        String OTP,
        @Password String newPassword,
        @Password String confirmPassword
) {
}
