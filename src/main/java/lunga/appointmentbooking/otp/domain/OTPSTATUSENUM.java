package lunga.appointmentbooking.otp.domain;

import java.util.Arrays;

public enum OTPSTATUSENUM {
    CREATED,
    EXPIRED,
    RENEWED,
    VALIDATED,
    VERIFIED,
    REVOKED;

    public static boolean isMember(String status) {
        String finalStatus = status.toUpperCase();
        return  Arrays.stream(OTPSTATUSENUM.values()).anyMatch(v->v.name().equals(finalStatus));
    }
}
