package capitec.branch.appointment.utils;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

public record PasswordValidator() implements ConstraintValidator<Password, String> {
    @Override
    public boolean isValid(String password, ConstraintValidatorContext constraintValidatorContext) {
        return StringUtils.isNotBlank(password) &&
                Pattern.matches(Validator.PASSWORD_REGEX,password);
    }
}
