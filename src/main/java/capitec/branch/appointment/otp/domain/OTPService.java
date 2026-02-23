package capitec.branch.appointment.otp.domain;

import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

public interface OTPService {
    OTP saveOTP(@Valid OTP otp);
    Set<OTP> find(String username);
    Optional<OTP> find(String username, String otp,OTPStatus status);

    boolean deleteUserOTP( String username);

    Optional<OTP> findLatestOTP(String username,LocalDateTime fromDate);

    boolean update(@Valid OTP otp2, OTPStatus oldStatus);
}
