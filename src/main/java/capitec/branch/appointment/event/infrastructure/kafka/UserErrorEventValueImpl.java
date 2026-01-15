package capitec.branch.appointment.event.infrastructure.kafka;

import capitec.branch.appointment.event.domain.UserErrorEvent;
import capitec.branch.appointment.kafka.user.UserErrorEventValue;
import capitec.branch.appointment.kafka.user.UserMetadata;

import java.time.LocalDateTime;

public record UserErrorEventValueImpl(
        UserErrorEvent event
) implements UserErrorEventValue {
    @Override
    public UserMetadata getMetadata() {
        return new UserMetadata(event.getFullname(),event.getUsername(),event.getEmail());
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
