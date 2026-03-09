package capitec.branch.appointment.utils;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import capitec.branch.appointment.otp.domain.OTP;
import org.apache.commons.lang3.StringUtils;


public class OTPCodeValidator implements ConstraintValidator<OTPCode, String> {
    @Override
    public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {

        return StringUtils.isNotBlank(s) &&
                StringUtils.length(s)== OTP.CODE_FIELD_LENGTH;
    }
    public static boolean isValid(String s) {

        return StringUtils.isNotBlank(s) &&
                StringUtils.length(s)== OTP.CODE_FIELD_LENGTH;
    }
}
