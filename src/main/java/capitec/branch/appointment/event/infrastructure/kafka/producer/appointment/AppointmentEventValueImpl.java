package capitec.branch.appointment.event.infrastructure.kafka.producer.appointment;

import capitec.branch.appointment.kafka.appointment.AppointmentEventValue;
import capitec.branch.appointment.kafka.appointment.AppointmentMetadata;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AppointmentEventValueImpl(
        String eventId,
        String topic,
        String value,
         String traceId,
         LocalDateTime publishTime,
        AppointmentMetadata metadata
) implements AppointmentEventValue {

    @Override
    public AppointmentMetadata getMetadata() {
        return metadata();
    }


    @Override
    public String getKey() {
        return metadata().id().toString();
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
        return eventId();
    }

    @Override
    public LocalDateTime getPublishTime() {
        return publishTime();
    }
    @Override
    public  String getSource() {
        return "Appointment context";
    }


}
