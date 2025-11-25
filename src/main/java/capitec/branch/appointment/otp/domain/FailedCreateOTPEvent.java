package capitec.branch.appointment.otp.domain;
import jakarta.validation.constraints.NotBlank;

public record FailedCreateOTPEvent(
       String username,
       @NotBlank String traceId
) {

}
