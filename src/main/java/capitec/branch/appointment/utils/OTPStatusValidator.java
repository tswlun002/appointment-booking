package capitec.branch.appointment.utils;


import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import capitec.branch.appointment.otp.domain.OTPSTATUSENUM;
import org.apache.commons.lang3.StringUtils;

public record OTPStatusValidator() implements ConstraintValidator<MemberOTPEnum, String> {
    @Override
    public boolean isValid(String status, ConstraintValidatorContext constraintValidatorContext) {
        return StringUtils.isNotBlank(status) && OTPSTATUSENUM.isMember(status);
    }
}
