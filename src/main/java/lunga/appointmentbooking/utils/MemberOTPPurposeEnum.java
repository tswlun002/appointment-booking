package lunga.appointmentbooking.utils;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import lunga.appointmentbooking.otp.domain.OTPSTATUSENUM;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

@Documented
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = OTPPurposeValidator.class)
public @interface MemberOTPPurposeEnum {
    String message() default "OTP purpose must be member of OTP  purpose enum ";
    @AliasFor(attribute = "name")
    OTPSTATUSENUM value() default OTPSTATUSENUM.CREATED;
    @AliasFor(attribute = "value")
    OTPSTATUSENUM name() default OTPSTATUSENUM.CREATED;
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
