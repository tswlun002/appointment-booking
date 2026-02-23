package capitec.branch.appointment.utils;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@java.lang.annotation.Target({java.lang.annotation.ElementType.TYPE})
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Service
@Validated
public @interface UseCase {
    @org.springframework.core.annotation.AliasFor(annotation = org.springframework.stereotype.Component.class)
    String value() default "";
}
