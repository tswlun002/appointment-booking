package capitec.branch.appointment.user.app.event;

import capitec.branch.appointment.utils.CustomerEmail;
import capitec.branch.appointment.utils.Username;
import capitec.branch.appointment.utils.ValidatorMessages;
import jakarta.validation.constraints.NotBlank;

public record UserCreatedEvent(
        @Username String username,
        @CustomerEmail String email,
        @NotBlank(message = ValidatorMessages.FIRSTNAME + " " + ValidatorMessages.LASTNAME)
        String fullname,
        @NotBlank(message = ValidatorMessages.EVENT_TRACE_ID_MESS)
        String traceId
) {
}
