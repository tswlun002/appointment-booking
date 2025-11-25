package capitec.branch.appointment.otp.app;


import capitec.branch.appointment.utils.Username;
import capitec.branch.appointment.utils.Validator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record OTPExpiredEvent(
        @Username String username,
        @NotBlank(message = Validator.EMAIL_MESS) @Email(message = Validator.EMAIL_MESS)
        String email,
        @NotBlank(message = Validator.EVENT_TRACE_ID_MESS)
        String traceId) {
}
