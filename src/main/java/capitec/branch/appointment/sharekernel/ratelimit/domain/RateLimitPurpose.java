package capitec.branch.appointment.sharekernel.ratelimit.domain;

/**
 * Enum representing different purposes for rate limiting.
 */
public enum RateLimitPurpose {
    OTP_RESEND,
    PASSWORD_RESET_REQUEST,
    LOGIN_ATTEMPT,
    API_REQUEST
}
