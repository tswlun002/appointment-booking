package capitec.branch.appointment.event.app.port;


public interface OTPPort {
    void verifyOTP(String otpValue, String username);

    String generateOTP(String username, String traceId, String purpose);
    void deleteOTP(String username,String traceId);
}
