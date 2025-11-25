package capitec.branch.appointment.otp.infrastructure;


import capitec.branch.appointment.utils.MemberOTPPurposeEnum;
import org.springframework.data.relational.core.mapping.Column;

public record OTPPurpose(
        @Column("purpose")
        @MemberOTPPurposeEnum
        String name
) {
}
