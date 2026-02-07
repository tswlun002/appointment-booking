package capitec.branch.appointment.user.app.event;

import capitec.branch.appointment.utils.Username;
import capitec.branch.appointment.utils.Validator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PasswordResetRequestEvent(
        @Username String username,
        @NotBlank(message = Validator.EMAIL_MESS) @Email(message = Validator.EMAIL_MESS)
        String email,
        @NotBlank(message = Validator.FIRSTNAME + " " + Validator.LASTNAME)
        String fullname,
        @NotBlank(message = Validator.EVENT_TRACE_ID_MESS)
        String traceId)  {
}
