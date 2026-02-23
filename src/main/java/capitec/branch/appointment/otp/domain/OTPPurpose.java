package capitec.branch.appointment.otp.domain;

public enum OTPPurpose {
    EMAIL_VERIFICATION,
    PASSWORD_RESET,
    ACCOUNT_DELETION;

    public static boolean isValueOf(String value) {
        for (OTPPurpose purpose : values()) {
            if (purpose.name().equals(value)) {
                return true;
            }
        }
        return false;
    }
}
