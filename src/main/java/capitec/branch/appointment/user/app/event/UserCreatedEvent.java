package capitec.branch.appointment.user.app.event;

import capitec.branch.appointment.utils.Username;
import capitec.branch.appointment.utils.ValidatorMessages;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserCreatedEvent(
        @Username String username,
        @NotBlank(message = ValidatorMessages.EMAIL_MESS)
        @Email(message = ValidatorMessages.EMAIL_MESS)  String email,
        @NotBlank(message = ValidatorMessages.FIRSTNAME + " " + ValidatorMessages.LASTNAME)
        String fullname,
        @NotBlank(message = ValidatorMessages.EVENT_TRACE_ID_MESS)
        String traceId
) {
}
