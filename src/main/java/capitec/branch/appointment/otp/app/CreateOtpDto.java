package capitec.branch.appointment.otp.app;

import capitec.branch.appointment.otp.domain.OTP_PURPOSE_ENUM;
import capitec.branch.appointment.user.domain.UsernameGenerator;
import capitec.branch.appointment.utils.Username;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.apache.http.util.Asserts;

public record CreateOtpDto(
        @Username
        String username,
        @NotBlank(message = "Trace ID cannot be blank")
        String traceId,
        @NotNull(message = "Purpose cannot be null")
        OTP_PURPOSE_ENUM purpose

) {

    public  CreateOtpDto {

        Asserts.notBlank(traceId, "Trace ID cannot be blank");
        Asserts.notNull(purpose, "Purpose cannot be null");
        Asserts.check(UsernameGenerator.isValid(username), "Username is not valid");
    }

}
