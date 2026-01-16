package capitec.branch.appointment.event.infrastructure.kafka.producer.user;

import capitec.branch.appointment.kafka.user.UserEventValue;
import capitec.branch.appointment.kafka.user.UserMetadata;

import java.time.LocalDateTime;

public record UserEventValueImpl(
        String eventId,
        String topic,
        String value,
         String traceId,
         LocalDateTime publishTime,
        UserMetadata metadata
) implements UserEventValue {

    @Override
    public UserMetadata getMetadata() {
        return metadata;
    }


    @Override
    public String getKey() {
        return metadata.username();
    }

    @Override
    public String getValue() {
        return value();
    }

    @Override
    public String getTraceId() {
        return traceId();
    }

    @Override
    public String getTopic() {
        return topic();
    }

    @Override
    public String getEventId() {
        return traceId();
    }

    @Override
    public LocalDateTime getPublishTime() {
        return publishTime();
    }


}
