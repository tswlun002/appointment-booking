package capitec.branch.appointment.utils;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import capitec.branch.appointment.otp.domain.OTP;
import org.apache.commons.lang3.StringUtils;


public class OTPCodeValidator implements ConstraintValidator<OTPCode, String> {
    @Override
    public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
        var value =StringUtils.isNotBlank(s) &&
                StringUtils.length(s)== OTP.CODE_FIELD_LENGTH;

        return value;
    }
}
