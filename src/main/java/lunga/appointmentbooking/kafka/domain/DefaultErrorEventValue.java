package lunga.appointmentbooking.kafka.domain;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Setter
@Getter
public abstract class DefaultErrorEventValue implements ErrorEventValue, Serializable {

    private @NonNull String topic;
    private String value;
    private String traceId;
    private String eventId;
    private LocalDateTime publishTime;
    private Long partition;
    private Long offset;
    private String key;
    private String headers;
    private String exception;
    private String exceptionClass;
    private String causeClass;
    private String stackTrace;
    private boolean retryable;
    private int retryCount;
    private DEAD_LETTER_STATUS deadLetterStatus;
    @JsonCreator
    public DefaultErrorEventValue(
            @JsonProperty("topic") @NonNull String topic,
            @JsonProperty("value") String value,
            @JsonProperty("traceId") String traceId,
            @JsonProperty("eventId") String eventId,
            @JsonProperty("publishTime") LocalDateTime publishTime,
            @JsonProperty("partition") Long partition,
            @JsonProperty("offset") Long offset,
            @JsonProperty("key") String key,
            @JsonProperty("exception") String exception,
            @JsonProperty("exceptionClass") String exceptionClass,
            @JsonProperty("causeClass") String causeClass,
            @JsonProperty("stackTrace") String stackTrace,
            @JsonProperty("retryable") boolean retryable,
            @JsonProperty("retryCount") int retryCount,
            @JsonProperty("deadLetterStatus") DEAD_LETTER_STATUS deadLetterStatus) {
    this.topic = topic;
        this.value = value;
        this.traceId = traceId;
        this.eventId = eventId;
        this.publishTime = publishTime;
        this.partition = partition;
        this.offset = offset;
        this.key = key;
        this.exception = exception;
        this.exceptionClass = exceptionClass;
        this.causeClass = causeClass;
        this.stackTrace = stackTrace;
        this.retryable = retryable;
        this.retryCount = retryCount;
        this.deadLetterStatus = deadLetterStatus;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public boolean equals(Object o) {

        if (!(o instanceof DefaultErrorEventValue that)) return false;

        return Objects.equals(eventId, that.eventId) ;
    }

    @Override
    public int hashCode() {

        return Objects.hash(topic, value, traceId, eventId, publishTime, partition, offset, key
                , headers, exception, exceptionClass, causeClass, stackTrace, retryable, retryCount, deadLetterStatus);
    }

    @Override
    public String toString() {
        try {
            ObjectMapper mapper = EventToJSONMapper.getMapper();
            return mapper.writeValueAsString(this);
        } catch (Exception e) {
            throw new RuntimeException("Error converting to JSON", e);
        }
    }


}
