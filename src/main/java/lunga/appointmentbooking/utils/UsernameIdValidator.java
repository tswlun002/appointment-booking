package lunga.appointmentbooking.utils;

import lunga.appointmentbooking.user.domain.UsernameGenerator;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class UsernameIdValidator implements ConstraintValidator<Username,String> {

    @Override
    public boolean isValid(String username, ConstraintValidatorContext constraintValidatorContext) {
        return UsernameGenerator.isValid(username);
    }
}
