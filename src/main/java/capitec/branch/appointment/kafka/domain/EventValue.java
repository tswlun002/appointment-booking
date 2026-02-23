package capitec.branch.appointment.kafka.domain;

import capitec.branch.appointment.sharekernel.event.metadata.AppointmentMetadata;
import capitec.branch.appointment.sharekernel.event.metadata.OTPMetadata;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.io.Serializable;
import java.time.LocalDateTime;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "@eventType"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = EventValue.OriginEventValue.class, name = "OriginEventValue"),
        @JsonSubTypes.Type(value = EventValue.EventError.class, name = "EventError")
})
public sealed interface EventValue<K,T> extends Serializable {


    boolean isError();
    T value();
    K key();
    String traceId();
    String topic();
    String eventId();
    LocalDateTime publishTime();
    default String schemaVersion() { return "1.0"; }
    default String source() { return "unknown"; }
    default String eventType() { return EventValue.class.getSimpleName(); }

    @JsonTypeName("OriginEventValue")
    record OriginEventValue<K,T>(
            K key,
            @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@type")
            @JsonSubTypes({
                    @JsonSubTypes.Type(value = OTPMetadata.class, name = "OTPMetadata"),
                    @JsonSubTypes.Type(value = AppointmentMetadata.class, name = "AppointmentMetadata")
            })
            T value,
            String traceId,
            String topic,
            String eventId,
            LocalDateTime publishTime,
            String schemaVersion,
            String source,
            String eventType
            ) implements EventValue<K,T>,Serializable {
        public OriginEventValue(
                K key,
                T value,
                String traceId,
                String topic,
                String eventId,
                LocalDateTime publishTime){
            this(key, value, traceId,topic,eventId,publishTime, null,null, EventError.class.getSimpleName());

        }

        @Override
        public boolean isError() {
            return false;
        }
    }

    @JsonTypeName("EventError")
    record EventError<K,T> (

            //original values
            K key,
            @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@type")
            @JsonSubTypes({
                    @JsonSubTypes.Type(value = OTPMetadata.class, name = "OTPMetadata"),
                    @JsonSubTypes.Type(value = AppointmentMetadata.class, name = "AppointmentMetadata")
            })
            T value,
            String traceId,
            String topic,
            String eventId,
            LocalDateTime publishTime,
            String schemaVersion,
            String source,
            String eventType,
            //Error values
            Long partition,
            Long offset,
            String exception,
            String exceptionClass,
            String causeClass,
            String stackTrace,
            boolean isRetryable
    ) implements EventValue<K,T>,Serializable {


        public EventError(
                //original values
                K key,
                T value,
                String traceId,
                String topic,
                String eventId,
                LocalDateTime publishTime,
                Long partition,
                Long offset,
                String exception,
                String exceptionClass,
                String causeClass,
                String stackTrace,
                boolean isRetryable
        ){
            this(key, value, traceId,topic,eventId,publishTime, null,null, EventError.class.getSimpleName(),partition,
                    offset,exception,exceptionClass,causeClass, stackTrace,isRetryable);

        }

        @Override
        public boolean isError() {
            return true;
        }
    }

}
