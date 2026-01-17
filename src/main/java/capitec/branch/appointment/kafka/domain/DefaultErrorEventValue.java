package capitec.branch.appointment.kafka.domain;


import capitec.branch.appointment.utils.sharekernel.EventToJSONMapper;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.NonNull;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Getter
public abstract class DefaultErrorEventValue implements ErrorEventValue, Serializable {
    public  static final ObjectMapper mapper = EventToJSONMapper.getMapper();
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
            @JsonProperty("retryable") boolean retryable
    ) {
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

        return Objects.hash(eventId);
    }

    @Override
    public String toString() {
        try {

            return mapper.writeValueAsString(this);
        } catch (Exception e) {
            throw new RuntimeException("Error converting to JSON", e);
        }
    }


}
