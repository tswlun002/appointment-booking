package capitec.branch.appointment.kafka.domain;

import java.io.Serializable;
import java.time.LocalDateTime;

public sealed interface EventValue<K,T> extends  Serializable{


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

    record OriginEventValue<K,T>(
            K key,
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

    record EventError<K,T> (

            //original values
            K key,
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
