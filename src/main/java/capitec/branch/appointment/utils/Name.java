package capitec.branch.appointment.utils;


import capitec.branch.appointment.user.domain.User;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NamesValidator.class)
public @interface Name {
    String message() default "Valid name must be at least"+ User.NAMES_FIELD_LENGTH+" characters long excluding special characters";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}