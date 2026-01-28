package capitec.branch.appointment.utils;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.LocalDate;

public class RSAIdValidator implements ConstraintValidator<ValidRSAId, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || !value.matches("\\d{13}")) {
            return false;
        }

        // 1. Date Validation
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyMMdd");
            LocalDate.parse(value.substring(0, 6), formatter);
        } catch (DateTimeParseException e) {
            return false;
        }

        // 2. Citizenship check (11th digit: 0, 1, or 2)
        int citizenship = Character.getNumericValue(value.charAt(10));
        if (citizenship < 0 || citizenship > 2) return false;

        // 3. Luhn Algorithm Checksum
        return validateLuhn(value);
    }

    private boolean validateLuhn(String id) {
        int sum = 0;
        for (int i = 0; i < id.length(); i++) {
            int digit = Character.getNumericValue(id.charAt(i));
            // Double every second digit from right
            if ((id.length() - i) % 2 == 0) {
                digit *= 2;
                if (digit > 9) digit -= 9;
            }
            sum += digit;
        }
        return (sum % 10 == 0);
    }
}