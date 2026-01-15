package capitec.branch.appointment.event.infrastructure.kafka;

import capitec.branch.appointment.event.domain.UserErrorEvent;
import capitec.branch.appointment.kafka.domain.ErrorEventValue;
import capitec.branch.appointment.kafka.user.UserErrorEventValue;

public class UserErrorEventMapper {


    public static ErrorEventValue toKafkaErrorEventValue(UserErrorEvent event) {
        return  new UserErrorEventValueImpl(event);
    }
    public static UserErrorEvent toKafkaErrorEventValue(UserErrorEventValue event) {
        return  UserErrorEvent.create(
                event.getEventId(),
                event.getKey(),
                event.getTopic(),
                event.getValue(),
                event.getTraceId(),
                event.getPublishTime(),
                event.getException(),
                event.getExceptionClass(),
                event.getCauseClass(),
                event.getStackTrace(),
                event.isRetryable(),
                event.getPartition(),
                event.getOffset(),
                event.getFullname(),
                event.getUsername(),
                event.getEmail()
        );
    }
}
