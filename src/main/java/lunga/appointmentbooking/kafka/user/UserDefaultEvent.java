package lunga.appointmentbooking.kafka.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lunga.appointmentbooking.kafka.domain.EventToJSONMapper;

import java.time.LocalDateTime;
@Getter
@Setter
public class UserDefaultEvent implements UserEventValue {

        @NotBlank(message = "Event name is required")
        private String fullname;
        @NotBlank(message = "username is required")
        private String username;

        @NotBlank(message = "Event must have unique key")
        private String key;

        @NotBlank(message="Email is required")
        @Email
        private String email;

        @NonNull String topic;

        @NotBlank
        private String traceId;

        @NotBlank
        private String eventId;

        private LocalDateTime publishTime;

        private  String value;

        @JsonCreator
        public UserDefaultEvent(
                @JsonProperty("topic") @NonNull String topic,
                @JsonProperty("value") String value,
                @JsonProperty("traceId") String traceId,
                @JsonProperty("eventId") String eventId,
                @JsonProperty("publishTime") LocalDateTime publishTime,
                @JsonProperty("fullname") String fullname,
                @JsonProperty("username") String username,
                @JsonProperty("key") String key,
                @JsonProperty("email") String email) {
                this.topic = topic;
                this.value = value;
                this.traceId = traceId;
                this.eventId = eventId;
                this.publishTime = publishTime;
                this.fullname = fullname;
                this.username = username;
                this.key = key;
                this.email = email;

        }
        public UserDefaultEvent(@NonNull String topic, String value, String traceId, String eventId, LocalDateTime publishTime,
                                String fullname, String username, String email) {


                this.topic = topic;
                this.value = value;
                this.traceId = traceId;
                this.eventId = eventId;
                this.publishTime = publishTime;
                this.fullname = fullname;
                this.username = username;
                this.key = username;
                this.email = email;

        }

        public UserDefaultEvent(@NonNull String topic, String value, String traceId, String eventId, LocalDateTime publishTime, String username, String email) {


                this.topic = topic;
                this.value = value;
                this.traceId = traceId;
                this.eventId = eventId;
                this.publishTime = publishTime;
                this.fullname = value;
                this.username = username;
                this.key = username;
                this.email = email;

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

        public String getFullname() {
                return fullname;
        }

        @Override
        public String getValue() {
                return value;
        }

        @Override
        public String getTraceId() {
                return traceId;
        }

        @Override
        public String getTopic() {
                return topic;
        }

        @Override
        public String getEventId() {
                return eventId;
        }

        @Override
        public LocalDateTime getPublishTime() {
                return publishTime;
        }
}
