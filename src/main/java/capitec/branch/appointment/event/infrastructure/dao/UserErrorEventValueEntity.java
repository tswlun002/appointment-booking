package capitec.branch.appointment.event.infrastructure.dao;

import capitec.branch.appointment.event.infrastructure.UserEventStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Embedded;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
@Table("user_dead_letter_event")
record UserErrorEventValueEntity(
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
    @Column("next_retry_at")
    LocalDateTime nextRetryAt,
    String exception,
    @Column("exception_class")
    String exceptionClass,
    @Column("exception_cause")
    String exceptionCause,
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
                entity.retryable(),entity.retryCount(),entity.nextRetryAt(),entity.exception(),entity.exceptionClass(),entity.exceptionCause(),
                entity.stackTrace(),entity.traceId(),entity.status(),entity.publishTime(),entity.fullname(),entity.username(),
                entity.email());
    }
//
//    public void setRetryCount(int count) {
//        Assert.isTrue(count >= 0, "Retry count must be non-negative");
//        this.retryCount = count;
//    }
//
//    public void markRecovered(Long partition, Long offset) {
//        Assert.notNull(partition, "Partition must not be null when marking as recovered");
//        Assert.notNull(offset, "Offset must not be null when marking as recovered");
//        Assert.isTrue(partition >= 0, "Partition must be non-negative");
//        Assert.isTrue(offset >= 0, "Offset must be non-negative");
//
//        this.status = new UserEventStatus(DEAD_LETTER_STATUS.RECOVERED.name());
//        this.partition = partition;
//        this.offset = offset;
//    }
//
//    public void markFailed() {
//        this.retryable = false;
//        this.status = new UserEventStatus(DEAD_LETTER_STATUS.DEAD.name());
//    }
//
//    public void markPending(LocalDateTime nextRetryAt) {
//        Assert.notNull(nextRetryAt, "Next retry time must not be null");
//        Assert.isTrue(nextRetryAt.isAfter(LocalDateTime.now()),
//                "Next retry time must be in the future");
//
//        this.nextRetryAt = nextRetryAt;
//    }
//
//    public void scheduleNextRetry(LocalDateTime nextRetryAt) {
//        Assert.notNull(nextRetryAt, "Next retry time must not be null");
//
//        incrementRetry();
//        markPending(nextRetryAt);
//    }

    // Copy constructor



}
