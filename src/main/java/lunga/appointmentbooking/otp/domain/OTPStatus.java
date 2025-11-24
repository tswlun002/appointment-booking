package lunga.appointmentbooking.otp.domain;


import lunga.appointmentbooking.utils.MemberOTPEnum;

public record OTPStatus(
        @MemberOTPEnum
        String status
) {
}
