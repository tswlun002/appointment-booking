package capitec.branch.appointment.event.infrastructure.kafka.producer.appointment;

import capitec.branch.appointment.event.domain.ErrorEvent;
import capitec.branch.appointment.kafka.appointment.AppointmentErrorEventValue;
import capitec.branch.appointment.kafka.appointment.AppointmentMetadata;
import capitec.branch.appointment.utils.EventToJSONMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.time.LocalDateTime;

public record AppointmentErrorEventValueImpl(
        ErrorEvent event
) implements AppointmentErrorEventValue {
    @Override
    public AppointmentMetadata getMetadata() {
        try {
            var data = event.parseData(AppointmentMetadata.class, EventToJSONMapper.getMapper());
            return new AppointmentMetadata(
                    data.id(),
                    data.reference(),
                    data.branchId(),
                    data.customerUsername(),
                    data.createdAt(),
                    data.otherData()
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Long getPartition() {
        return event.getRecoveredPartition();
    }

    @Override
    public Long getOffset() {
        return event.getRecoveredOffset();
    }

    @Override
    public String getException() {
        return event.getException();
    }

    @Override
    public String getExceptionClass() {
        return event.getExceptionClass();
    }

    @Override
    public String getCauseClass() {
        return event.getExceptionCause();
    }

    @Override
    public String getStackTrace() {
        return event.getStackTrace();
    }

    @Override
    public boolean isRetryable() {
        return event.isRetryable();
    }

    @Override
    public String getKey() {
        return event.getKey();
    }

    @Override
    public String getValue() {
        return event.toString();
    }

    @Override
    public String getTraceId() {
        return event.getTraceId();
    }

    @Override
    public String getTopic() {
        return event.getTopic();
    }

    @Override
    public String getEventId() {
        return event.getEventId();
    }

    @Override
    public LocalDateTime getPublishTime() {
        return event.getTimestamp();
    }
}
