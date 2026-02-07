package capitec.branch.appointment.user.app.port;

/**
 * Port for rate limiting OTP resend requests.
 * Implemented by shared rate limit infrastructure.
 */
public interface OtpResendRateLimitPort {

    /**
     * Checks if OTP resend is allowed for the given username.
     *
     * @param username the username to check
     * @return true if resend is allowed, false if rate limit exceeded
     */
    boolean isResendAllowed(String username);

    /**
     * Checks if cooldown period has passed since last OTP resend.
     *
     * @param username the username to check
     * @return true if cooldown has passed, false if still in cooldown
     */
    boolean isCooldownPassed(String username);

    /**
     * Records an OTP resend attempt for the given username.
     *
     * @param username the username
     */
    void recordResendAttempt(String username);

    /**
     * Gets the remaining seconds until the rate limit window resets.
     *
     * @param username the username
     * @return seconds until reset, or 0 if not rate limited
     */
    long getSecondsUntilReset(String username);

    /**
     * Resets the rate limit for the given username (e.g., after successful verification).
     *
     * @param username the username
     */
    void reset(String username);
}
