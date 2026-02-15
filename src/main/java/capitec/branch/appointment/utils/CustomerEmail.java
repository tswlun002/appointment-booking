package capitec.branch.appointment.utils;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that the annotated string is a valid, non-blank email address.
 *
 * This combines:
 * - NotBlank validation (must not be null, empty, or whitespace only)
 * - Email format validation (must be valid email format)
 */
@Documented
@Constraint(validatedBy = NotBlankEmailValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD, ElementType.ANNOTATION_TYPE, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface CustomerEmail {

    String message() default ValidatorMessages.EMAIL_MESS;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

