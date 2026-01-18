package capitec.branch.appointment.event.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.util.Asserts;

import java.time.LocalDateTime;
import java.util.Objects;

public class ErrorEvent {

    private final String eventId;
    private final String key;
    private final String topic;
    private final String value;
    private final String traceId;
    private final LocalDateTime timestamp;
    private final String exception;
    private final String exceptionClass;
    private final String exceptionCause;
    private final String stackTrace;
    private int retryCount;
    private boolean retryable;
    private LocalDateTime nextRetryAt;
    private DEAD_LETTER_STATUS status;
    private Long recoveredPartition;
    private Long recoveredOffset;

    private ErrorEvent(String eventId, String key, String topic, String value,
                       String traceId, LocalDateTime timestamp, String exception,
                       String exceptionClass, String exceptionCause, String stackTrace, int retryCount,
                       boolean retryable, LocalDateTime nextRetryAt,
                       DEAD_LETTER_STATUS status, Long recoveredPartition,
                       Long recoveredOffset) {
        Asserts.notBlank(eventId, "eventId");
        Asserts.notBlank(key, "key");
        Asserts.notBlank(topic, "topic");
        Asserts.notBlank(value, "value");
        Asserts.notNull(timestamp, "timestamp");
        Asserts.notBlank(exception, "exception");
        Asserts.notNull(status, "status");

        this.eventId = eventId;
        this.key = key;
        this.topic = topic;
        this.value = value;
        this.traceId = traceId;
        this.timestamp = timestamp;
        this.exception = exception;
        this.exceptionClass = exceptionClass;
        this.stackTrace = stackTrace;
        this.retryCount = retryCount;
        this.retryable = retryable;
        this.nextRetryAt = nextRetryAt;
        this.status = status;
        this.recoveredPartition = recoveredPartition;
        this.recoveredOffset = recoveredOffset;
        this.exceptionCause = exceptionCause;
    }

    // Factory for new events (with optional partition/offset)
    public static  ErrorEvent create(String eventId, String key, String topic, String value,
                                    String traceId, LocalDateTime timestamp, String exception,
                                    String exceptionClass, String exceptionCause, String stackTrace, boolean retryable,
                                    Long partition, Long offset) {
        return new ErrorEvent(eventId, key, topic, value, traceId, timestamp,
                exception, exceptionClass,  exceptionCause,stackTrace, 0, retryable, null,
                DEAD_LETTER_STATUS.DEAD, partition, offset);
    }

    // Reconstitution from persistence


    public static ErrorEvent reconstitute(String eventId, String key, String topic, String value,
                                          String traceId, LocalDateTime timestamp, String exception,
                                          String exceptionClass, String exceptionCause, String stackTrace, int retryCount,
                                          boolean retryable, LocalDateTime nextRetryAt,
                                          DEAD_LETTER_STATUS status, Long recoveredPartition,
                                          Long recoveredOffset) {
        return new ErrorEvent(eventId, key, topic, value, traceId, timestamp,
                exception, exceptionClass,exceptionCause, stackTrace, retryCount, retryable, nextRetryAt,
                status, recoveredPartition, recoveredOffset);
    }

    // Domain behavior
    public void incrementRetry(int maxRetry) {
        Asserts.check(maxRetry > 0, "Max retry must be greater than 0");
        Asserts.check(retryCount < maxRetry, "Retry count must be less than max retry");
        this.retryCount++;
    }

    public void markRecovered(Long partition, Long offset) {
        Asserts.notNull(partition, "partition");
        Asserts.notNull(offset, "offset");
        this.status = DEAD_LETTER_STATUS.RECOVERED;
        this.retryable = false;
        this.recoveredPartition = partition;
        this.recoveredOffset = offset;
    }

    public void markFailed() {
        this.status = DEAD_LETTER_STATUS.DEAD;
        this.retryable = false;
    }

    public void scheduleNextRetry(LocalDateTime nextRetry) {
        Asserts.notNull(nextRetry, "nextRetry");
        Asserts.check(nextRetry.isAfter(LocalDateTime.now()), "nextRetry must be in the future");
        this.retryable = true;
        this.nextRetryAt = nextRetry;
    }

    public boolean canRetry(int maxRetry) {
        Asserts.check(maxRetry > 0, "Max retry must be greater than 0");
        return retryable && retryCount < maxRetry;
    }

    public boolean isPendingRetry() {
        return retryable && retryCount > 0;
    }

    public <R> R parseData(Class<R> clazz, ObjectMapper mapper) throws JsonProcessingException {
        return mapper.readValue(value, clazz);
    }

    // Getters
    public String getEventId() { return eventId; }
    public String getKey() { return key; }
    public String getTopic() { return topic; }
    public String getValue() { return value; }
    public String getTraceId() { return traceId; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getException() { return exception; }
    public String getExceptionClass() { return exceptionClass; }
    public String getStackTrace() { return stackTrace; }
    public int getRetryCount() { return retryCount; }
    public boolean isRetryable() { return retryable; }
    public LocalDateTime getNextRetryAt() { return nextRetryAt; }
    public DEAD_LETTER_STATUS getStatus() { return status; }
    public Long getRecoveredPartition() { return recoveredPartition; }
    public Long getRecoveredOffset() { return recoveredOffset; }

    public String getExceptionCause() {
        return exceptionCause;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ErrorEvent that)) return false;
        return Objects.equals(eventId, that.eventId) && Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId, key);
    }

    @Override
    public String toString() {
        return "ErrorEvent{" +
                "eventId='" + eventId + '\'' +
                ", key='" + key + '\'' +
                ", topic='" + topic + '\'' +
                ", value='" + value + '\'' +
                ", traceId='" + traceId + '\'' +
                ", timestamp=" + timestamp +
                ", exception='" + exception + '\'' +
                ", exceptionClass='" + exceptionClass + '\'' +
                ", exceptionCause='" + exceptionCause + '\''+
                ", stackTrace='" + stackTrace + '\'' +
                ", retryCount=" + retryCount +
                ", retryable=" + retryable +
                ", nextRetryAt=" + nextRetryAt +
                ", status=" + status +
                ", recoveredPartition=" + recoveredPartition +
                ", recoveredOffset=" + recoveredOffset +
                '}';
    }
}
