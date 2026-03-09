package capitec.branch.appointment.utils;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.internal.constraintvalidators.AbstractEmailValidator;

/**
 * Validator for {@link CustomerEmail} annotation.
 *
 * Validates that the email:
 * 1. Is not null
 * 2. Is not blank (empty or whitespace only)
 * 3. Is a valid email format (using Hibernate's email validation)
 */
public class NotBlankEmailValidator implements ConstraintValidator<CustomerEmail, CharSequence> {

    /**
     * Inner class that extends Hibernate's AbstractEmailValidator to reuse email format validation.
     */
    private static class EmailFormatValidator extends AbstractEmailValidator<CustomerEmail> {
        @Override
        public void initialize(CustomerEmail constraintAnnotation) {
            // No additional initialization needed
        }
    }

    private final EmailFormatValidator emailFormatValidator = new EmailFormatValidator();

    @Override
    public void initialize(CustomerEmail constraintAnnotation) {
        emailFormatValidator.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        if (StringUtils.isBlank(value)) {
            return false;
        }

        // Check email format using Hibernate's validator
        return emailFormatValidator.isValid(value, context);
    }

    public static boolean isValid(CharSequence value) {
        if (StringUtils.isBlank(value)) {
            return false;
        }

        // Check email format using Hibernate's validator
        return new EmailFormatValidator().isValid(value, null);
    }
}

