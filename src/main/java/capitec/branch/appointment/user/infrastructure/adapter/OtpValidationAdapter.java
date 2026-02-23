package capitec.branch.appointment.user.infrastructure.adapter;

import capitec.branch.appointment.otp.app.ValidateOTPService;
import capitec.branch.appointment.user.app.port.OtpValidationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OtpValidationAdapter implements OtpValidationPort {

    private final ValidateOTPService validateOTPService;

    @Override
    public boolean validateOtp(String username, String otp, String traceId) {
        return validateOTPService.validateOTP(username, otp, traceId);
    }
}
