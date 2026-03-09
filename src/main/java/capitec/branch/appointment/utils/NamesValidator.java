package capitec.branch.appointment.utils;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

public class NamesValidator implements ConstraintValidator<Name, String> {
    private static final String NAME_REGEX = "^[A-Za-z]{2,}(?:\\s[A-Za-z]+)*$";
    @Override
    public boolean isValid(String name, ConstraintValidatorContext constraintValidatorContext) {
        return StringUtils.isNotBlank(name) && name.matches(NAME_REGEX);
    }
    public static boolean isValid(String name) {
        return StringUtils.isNotBlank(name) && name.matches(NAME_REGEX);
    }
}
