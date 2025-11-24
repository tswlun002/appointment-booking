package lunga.appointmentbooking.event.infrastructure.dao;


import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface FailedRecordRepository extends CrudRepository<UserErrorEventValueEntity, String> {
    @Query("""
            SELECT u.event_id, u.key, u.value, u.topic, u.partition, u.event_offset, u.headers, 
                u.is_retryable, u.retry_count, u.exception, u.exception_class, u.cause_class, 
                u.stack_trace, u.trace_id, u.status, u.published_time, 
                u.created_date, u.last_modified_date,u.fullname, u.email,u.username
                FROM user_dead_letter_event AS u WHERE u.status = :status
            """)
    Set<UserErrorEventValueEntity> findAllByStatus(@Param("status") String status);

    @Query("""
            SELECT u.event_id, u.key, u.value, u.topic, u.partition, u.event_offset, u.headers, 
                u.is_retryable, u.retry_count, u.exception, u.exception_class, u.cause_class, 
                u.stack_trace, u.trace_id, u.status, u.published_time,
                , u.created_date, u.last_modified_date,u.fullname, u.email,u.username
                FROM user_dead_letter_event AS u WHERE u.key = :key AND u.status = :status
            """)
    Set<UserErrorEventValueEntity> findByKeyAndStatus(@Param("key") String key, @Param("status") String status);

    @Query("""
            SELECT u.event_id, u.key, u.value, u.topic, u.partition, u.event_offset, u.headers, 
                u.is_retryable, u.retry_count, u.exception, u.exception_class, u.cause_class, 
                u.stack_trace, u.trace_id, u.status, u.published_time,
                 u.created_date, u.last_modified_date,u.fullname, u.email,u.username
                FROM user_dead_letter_event AS u WHERE u.event_id = :eventId
            """)
    Optional<UserErrorEventValueEntity> findByEventId(String eventId);
    @Query("""
            SELECT u.event_id, u.key, u.value, u.topic, u.partition, u.event_offset, u.headers, 
                u.is_retryable, u.retry_count, u.exception, u.exception_class, u.cause_class, 
                u.stack_trace, u.trace_id, u.status, u.published_time,
                u.created_date, u.last_modified_date,u.fullname, u.email,u.username
                FROM user_dead_letter_event AS u WHERE u.is_retryable=:retryable AND u.status = :status
            """)
    Set<UserErrorEventValueEntity> recoverDeadLetter(@Param("retryable") boolean retryable,@Param("status") String status);
}
