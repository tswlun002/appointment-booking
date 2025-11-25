package capitec.branch.appointment.event.infrastructure.dao;


import capitec.branch.appointment.event.infrastructure.UserEventStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Embedded;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
@Table("user_dead_letter_event")
public record UserErrorEventValueEntity(
    @Id
    @Column("event_id")
    String eventId,
    String key,
    String value,
    String topic,
    Long partition,
    @Column("event_offset")
    Long offset,
    String headers,
    @Column("is_retryable")
    Boolean retryable,
    @Column("retry_count")
    Integer retryCount,
    String exception,
    @Column("exception_class")
    String exceptionClass,
    @Column("cause_class")
    String causeClass,
    @Column("stack_trace")
    String stackTrace,
    @Column("trace_id")
    String traceId,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    UserEventStatus status,
    @Column("published_time")
    LocalDateTime publishTime,
    String fullname,
    String username,
    String email

    
){

    public  UserErrorEventValueEntity (String entityId, UserErrorEventValueEntity entity){
        this(entityId,entity.key(),entity.value(),entity.topic(),entity.partition(),entity.offset(),entity.headers(),
                entity.retryable(),entity.retryCount(),entity.exception(),entity.exceptionClass(),entity.causeClass(),
                entity.stackTrace(),entity.traceId(),entity.status(),entity.publishTime(),entity.fullname(),entity.username(),
                entity.email());
    }
}
