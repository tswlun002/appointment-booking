package capitec.branch.appointment.otp.domain;

public enum OTP_PURPOSE_ENUM {
    EMAIL_VERIFICATION,
    PASSWORD_RESET,
    ACCOUNT_DELETION;

    public static boolean isValueOf(String value) {
        for (OTP_PURPOSE_ENUM purpose : values()) {
            if (purpose.name().equals(value)) {
                return true;
            }
        }
        return false;
    }
}
