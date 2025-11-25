package capitec.branch.appointment.utils;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

public record RoleTypeValidator() implements ConstraintValidator<GroupName,String> {
    @Override
    public boolean isValid(String o, ConstraintValidatorContext constraintValidatorContext) {
        return StringUtils.isNotBlank(o)&&
                o.matches(Validator.ROLE_TYPE_NAME_REGEX);
    }
}
