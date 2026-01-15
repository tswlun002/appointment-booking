package capitec.branch.appointment.kafka.infrastructure.event;

import capitec.branch.appointment.kafka.domain.*;
import lombok.NonNull;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Default implementation of EventValueFactory using anonymous classes
 * to instantiate abstract domain event classes.
 */
@Component
public class DefaultEventValueFactory implements EventValueFactory {

    @Override
    public EventValue createEventValue(@NonNull String topic, String value, String traceId) {
        return new DefaultEventValue(topic, value, traceId) {};
    }

    @Override
    public ErrorEventValue createErrorEventValue(
            @NonNull String topic,
            String value,
            String traceId,
            String eventId,
            LocalDateTime publishTime,
            Long partition,
            Long offset,
            String key,
            String exception,
            String exceptionClass,
            String causeClass,
            String stackTrace,
            boolean retryable) {

        return new DefaultErrorEventValue(
                topic, value, traceId, eventId, publishTime,
                partition, offset, key, exception, exceptionClass,
                causeClass, stackTrace, retryable

        ) {};
    }


}

