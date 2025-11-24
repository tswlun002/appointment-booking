package lunga.appointmentbooking.otp.domain;

import java.util.Optional;
import java.util.Set;

public interface OTPService {
    OTP saveOTP(OTP otp);
    void verify(String otp, String username);
    Set<OTP> find(String username);
    Optional<OTP> find(String username, String otp);

    void renewOTP(OTP opt);
    Optional<OTP> validateOTP(String username, String otpCode, int maxAttempts);

    boolean deleteAllOTP(String traceId);

    boolean deleteUserOTP( String username);
}
