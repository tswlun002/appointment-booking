package capitec.branch.appointment.kafka.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import capitec.branch.appointment.kafka.domain.DEAD_LETTER_STATUS;
import capitec.branch.appointment.kafka.domain.DefaultErrorEventValue;
import capitec.branch.appointment.kafka.domain.EventToJSONMapper;

import java.time.LocalDateTime;

@Setter
@Getter
public class UserDefaultErrorEvent extends DefaultErrorEventValue {

    @NotBlank(message = "Event name is required")
    private String fullname;
    private String value;
    @NotBlank(message = "username is required")
    private String username;
    @NotBlank(message="Email is required")
    @Email
    private String email;

    @JsonCreator
    public UserDefaultErrorEvent(

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
            @JsonProperty("deadLetterStatus") DEAD_LETTER_STATUS deadLetterStatus,
            @JsonProperty("fullname") String fullname,
            @JsonProperty("username") String username,
            @JsonProperty("email") String email) {

        super(topic, value, traceId, eventId, publishTime, partition, offset, key, exception, exceptionClass, causeClass, stackTrace, retryable, retryCount, deadLetterStatus);
        this.fullname = fullname;
        this.username = username;
        this.email = email;
        this.value = value;
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
