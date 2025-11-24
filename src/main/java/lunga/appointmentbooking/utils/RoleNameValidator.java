package lunga.appointmentbooking.utils;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

public class RoleNameValidator implements ConstraintValidator<RoleName, String> {
    @Override
    public boolean isValid(String roleName, ConstraintValidatorContext constraintValidatorContext) {
        // role name must be ChanelName_Function_Entity , example: BLOCK_CREAT_USER, BLOCKADMIN_CREAT_CLIENT
        return StringUtils.isNotBlank(roleName)&&
                roleName.matches(Validator.ROLE_NAME_REGEX);
    }
}
