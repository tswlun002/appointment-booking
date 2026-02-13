package capitec.branch.appointment.user.app.event;

import capitec.branch.appointment.utils.Username;
import capitec.branch.appointment.utils.ValidatorMessages;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record DeleteUserEvent(
        @Username String username,
        @NotBlank(message = ValidatorMessages.EMAIL_MESS)
        @Email( message = ValidatorMessages.EMAIL_MESS)
        String email,
        String fullname,
        String OTP,
        String traceId
) {
}
