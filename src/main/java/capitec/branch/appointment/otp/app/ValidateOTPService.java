package capitec.branch.appointment.otp.app;

import capitec.branch.appointment.utils.Username;

public interface ValidateOTPService {
    boolean validateOTP( @Username String username, String otp, String traceId);
}
