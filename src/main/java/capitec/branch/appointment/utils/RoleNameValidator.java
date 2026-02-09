package capitec.branch.appointment.utils;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

public class RoleNameValidator implements ConstraintValidator<RoleName, String> {
    @Override
    public boolean isValid(String roleName, ConstraintValidatorContext constraintValidatorContext) {
        return StringUtils.isNotBlank(roleName)&&
                roleName.matches(Validator.ROLE_NAME_REGEX);
    }
}
