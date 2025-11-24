package lunga.appointmentbooking.utils;


import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lunga.appointmentbooking.otp.domain.OTP_PURPOSE_ENUM;
import org.apache.commons.lang3.StringUtils;

public class OTPPurposeValidator implements ConstraintValidator<MemberOTPPurposeEnum,String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {
        return StringUtils.isNotBlank(value) && OTP_PURPOSE_ENUM.isValueOf(value);
    }
}
