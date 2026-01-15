package capitec.branch.appointment.kafka.domain;

/**
 * Factory interface for creating event values.
 * Implementations should be in infrastructure layer since they create
 * concrete instances of abstract domain classes.
 */
public interface EventValueFactory {

    EventValue createEventValue(String topic, String value, String traceId);

    ErrorEventValue createErrorEventValue(
            String topic,
            String value,
            String traceId,
            String eventId,
            java.time.LocalDateTime publishTime,
            Long partition,
            Long offset,
            String key,
            String exception,
            String exceptionClass,
            String causeClass,
            String stackTrace,
            boolean retryable
    );

}
