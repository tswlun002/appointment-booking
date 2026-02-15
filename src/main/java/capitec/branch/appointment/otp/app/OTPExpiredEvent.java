package capitec.branch.appointment.otp.app;

import capitec.branch.appointment.utils.CustomerEmail;
import capitec.branch.appointment.utils.Username;
import capitec.branch.appointment.utils.ValidatorMessages;
import jakarta.validation.constraints.NotBlank;

public record OTPExpiredEvent(
        @Username String username,
        @CustomerEmail
        String email,
        @NotBlank(message = ValidatorMessages.EVENT_TRACE_ID_MESS)
        String traceId) {
}
