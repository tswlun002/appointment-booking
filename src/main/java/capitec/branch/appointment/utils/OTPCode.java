package capitec.branch.appointment.utils;


import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import capitec.branch.appointment.otp.domain.OTP;

import java.lang.annotation.*;

@Documented
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = OTPCodeValidator.class)
public @interface OTPCode {
    String message() default "OTP code must be "+ OTP.CODE_FIELD_LENGTH+" characters long";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}