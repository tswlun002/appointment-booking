package lunga.appointmentbooking.otp.infrastructure;


import lunga.appointmentbooking.utils.MemberOTPPurposeEnum;
import org.springframework.data.relational.core.mapping.Column;

public record OTPPurpose(
        @Column("purpose")
        @MemberOTPPurposeEnum
        String name
) {
}
