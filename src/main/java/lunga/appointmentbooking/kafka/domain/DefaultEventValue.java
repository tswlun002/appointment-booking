package lunga.appointmentbooking.kafka.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public  abstract class DefaultEventValue implements EventValue, Serializable {

        @NotBlank(message = "Topic name is required")
        private final String topic;
        @NotBlank(message = "Event must have purpose(values)")
        private final String value;
        @NotBlank(message = "Trace id is required for event")
        private final String traceId;
        private final String eventId;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyy-MM-dd'T'HH:mm:ss")
        private final LocalDateTime publishTime;
        @JsonCreator
        public DefaultEventValue(
                @JsonProperty("topic") String topic,
                @JsonProperty("value") String value,
                @JsonProperty("traceId") String traceId,
                @JsonProperty("eventId") String eventId,
                @JsonProperty("publishTime") LocalDateTime publishTime) {
                this.topic = topic;
                this.value = value;
                this.traceId = traceId;
                this.eventId = eventId;
                this.publishTime = publishTime;
        }

        public DefaultEventValue(@NonNull String topic, String value, String traceId) {
                this.topic = topic;
                this.value = value;
                this.traceId = traceId;
                this.eventId = UUID.randomUUID().toString();
                this.publishTime = LocalDateTime.now();
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

        @Override
        public String getKey() {
                return eventId;
        }
}
