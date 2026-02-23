package capitec.branch.appointment.kafka.domain;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

@Target({ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ConditionalOnProperty
public @interface PersistDeadLetterDB {
    @AliasFor(annotation = ConditionalOnProperty.class, attribute = "name")
    String name() default "kafka.error.persistence.enabled";

    @AliasFor(annotation = ConditionalOnProperty.class, attribute = "havingValue")
    String havingValue() default "true";

    @AliasFor(annotation = ConditionalOnProperty.class, attribute = "matchIfMissing")
    boolean matchIfMissing() default false;
}

