package capitec.branch.appointment.user.app.port;

/**
 * Port for OTP validation operations.
 * Implemented by OTP context infrastructure.
 */
public interface OtpValidationPort {

    /**
     * Validates the OTP for a user.
     *
     * @param username the username
     * @param otp      the OTP code to validate
     * @param traceId  trace identifier for logging
     * @return true if OTP is valid, false otherwise
     */
    boolean validateOtp(String username, String otp, String traceId);
}
