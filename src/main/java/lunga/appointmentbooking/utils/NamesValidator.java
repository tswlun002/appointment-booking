package lunga.appointmentbooking.utils;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

public class NamesValidator implements ConstraintValidator<Name, String> {
    @Override
    public boolean isValid(String name, ConstraintValidatorContext constraintValidatorContext) {
        return StringUtils.isNotBlank(name) && name.matches("[a-zA-Z]{2,}");
    }
}
