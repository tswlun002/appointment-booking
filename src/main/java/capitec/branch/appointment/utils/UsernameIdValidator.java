package capitec.branch.appointment.utils;

import capitec.branch.appointment.sharekernel.username.UsernameGenerator;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class UsernameIdValidator implements ConstraintValidator<Username,String> {

    @Override
    public boolean isValid(String username, ConstraintValidatorContext constraintValidatorContext) {
        return UsernameGenerator.isValid(username);
    }
}
