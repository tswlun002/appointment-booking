package capitec.branch.appointment.user.app.event;

import capitec.branch.appointment.utils.Username;
import capitec.branch.appointment.utils.Validator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record DeleteUserEvent(
        @Username String username,
        @NotBlank(message = Validator.EMAIL_MESS)
        @Email( message = Validator.EMAIL_MESS)
        String email,
        String fullname,
        String OTP,
        String traceId
) {
}
