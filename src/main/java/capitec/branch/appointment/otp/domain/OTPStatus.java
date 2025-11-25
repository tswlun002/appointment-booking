package capitec.branch.appointment.otp.domain;


import capitec.branch.appointment.utils.MemberOTPEnum;

public record OTPStatus(
        @MemberOTPEnum
        String status
) {
}
