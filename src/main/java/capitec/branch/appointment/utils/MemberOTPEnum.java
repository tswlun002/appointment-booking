package capitec.branch.appointment.utils;


import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import capitec.branch.appointment.otp.domain.OTPSTATUSENUM;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

@Documented
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = OTPStatusValidator.class)
public @interface MemberOTPEnum {
    String message() default "OTP status must be member of OTP  status enum ";
    @AliasFor(attribute = "name")
    OTPSTATUSENUM value() default OTPSTATUSENUM.CREATED;
    @AliasFor(attribute = "value")
    OTPSTATUSENUM name() default OTPSTATUSENUM.CREATED;
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
