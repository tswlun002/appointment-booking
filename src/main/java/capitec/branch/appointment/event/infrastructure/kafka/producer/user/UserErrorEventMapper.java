package capitec.branch.appointment.event.infrastructure.kafka.producer.user;

import capitec.branch.appointment.event.domain.ErrorEvent;
import capitec.branch.appointment.kafka.domain.ErrorEventValue;
import capitec.branch.appointment.kafka.user.UserErrorEventValue;
import capitec.branch.appointment.utils.sharekernel.EventToJSONMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

public class UserErrorEventMapper {


    public static ErrorEventValue toKafkaErrorEventValue(ErrorEvent event) {
        return  new UserErrorEventValueImpl(event);
    }
    public static ErrorEvent toKafkaErrorEventValue(UserErrorEventValue event) throws JsonProcessingException {
        return  ErrorEvent.create(
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
                EventToJSONMapper.getMapper().writeValueAsString(event.getMetadata())
        );
    }
}
