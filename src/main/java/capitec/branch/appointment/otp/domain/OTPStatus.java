package capitec.branch.appointment.otp.domain;

import java.util.Arrays;

public enum OTPStatus {
    CREATED("CREATED"),
    EXPIRED("EXPIRED"),
    RENEWED("RENEWED"),
    VALIDATED("VALIDATED"),
    VERIFIED("VERIFIED"),
    REVOKED("REVOKED");
    private final String value;
    OTPStatus(String status) {
        this.value = status;
    }
    public String getValue() {
        return value;
    }

    public static boolean isMember(String status) {
        String finalStatus = status.toUpperCase();
        return  Arrays.stream(OTPStatus.values()).anyMatch(v->v.name().equals(finalStatus));
    }
}
