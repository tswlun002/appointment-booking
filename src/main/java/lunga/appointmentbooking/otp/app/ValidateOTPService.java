package lunga.appointmentbooking.otp.app;

import lunga.appointmentbooking.utils.Username;

public interface ValidateOTPService {
    boolean validateOTP( @Username String username, String otp, String traceId);
}
