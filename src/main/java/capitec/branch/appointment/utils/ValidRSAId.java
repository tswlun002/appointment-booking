package capitec.branch.appointment.utils;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = RSAIdValidator.class) // Links to the logic below
public @interface ValidRSAId {
    String message() default "Invalid South African ID number";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}