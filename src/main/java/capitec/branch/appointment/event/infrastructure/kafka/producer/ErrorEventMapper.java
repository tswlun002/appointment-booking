package capitec.branch.appointment.event.infrastructure.kafka.producer;

import capitec.branch.appointment.event.domain.ErrorEvent;
import capitec.branch.appointment.kafka.domain.EventValue;
import capitec.branch.appointment.utils.sharekernel.EventToJSONMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.errors.SerializationException;

import java.io.Serializable;

public class ErrorEventMapper {


    public static < K extends Serializable,V extends Serializable> EventValue.EventError<K,V> toKafka(ErrorEvent event)  {
        ObjectMapper mapper = EventToJSONMapper.getMapper();
        try {
            TypeReference<K> kTypeReference=  new TypeReference<>() {};

            K k = mapper.readValue(event.getKey(), kTypeReference);
            TypeReference<V> vTypeReference=  new TypeReference<>() {};
            V v = mapper.readValue(event.getValue(),vTypeReference);

            return  new EventValue.EventError<K, V>(k, v,event.getTraceId(),event.getTopic(),event.getEventId(),
                    event.getTimestamp(),event.getRecoveredPartition(),event.getRecoveredOffset(),event.getException(),
                    event.getExceptionClass(),event.getExceptionCause(),event.getStackTrace(), event.isRetryable()
            );
        } catch (JsonProcessingException e) {
            throw new SerializationException("Error converting to ErrorEvent", e);

        }
    }

    public static < K extends Serializable,V extends Serializable> ErrorEvent toDomain(EventValue.EventError<K, V> event)  {
        ObjectMapper mapper = EventToJSONMapper.getMapper();
        try {
            return  ErrorEvent.create(
                    event.eventId(),
                    mapper.writeValueAsString(event.key()),
                    event.topic(),
                    mapper.writeValueAsString(event.value()),
                    event.traceId(),
                    event.publishTime(),
                    event.exception(),
                    event.exceptionClass(),
                    event.causeClass(),
                    event.stackTrace(),
                    event.isRetryable(),
                    event.partition(),
                    event.offset()
            );
        } catch (JsonProcessingException e) {
            throw new SerializationException("Error converting to ErrorEvent", e);

        }

    }
}
