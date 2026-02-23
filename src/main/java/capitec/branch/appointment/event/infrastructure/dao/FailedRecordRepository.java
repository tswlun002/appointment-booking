package capitec.branch.appointment.event.infrastructure.dao;


import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.List;

@Repository
interface FailedRecordRepository extends CrudRepository<UserErrorEventValueEntity, String> {
    @Query("""
            SELECT u.event_id, u.key, u.value, u.topic, u.partition, u.event_offset, u.headers, 
                u.is_retryable, u.retry_count, u.exception, u.exception_class, u.exception_cause, 
                u.stack_trace, u.trace_id, u.status, u.published_time, 
                u.created_date, u.last_modified_date
                FROM user_dead_letter_event AS u
                WHERE u.status = :status 
                ORDER BY created_date ASC 
                OFFSET :offset LIMIT :limit
            """)
    List<UserErrorEventValueEntity> findAllByStatus(@Param("status") String status,
                                                   @Param("offset") int offset,
                                                   @Param("limit") int limit);



    @Query("""
            SELECT u.event_id, u.key, u.value, u.topic, u.partition, u.event_offset, u.headers, 
                u.is_retryable, u.retry_count, u.exception, u.exception_class, u.exception_cause, 
                u.stack_trace, u.trace_id, u.status, u.published_time,
                u.created_date, u.last_modified_date
                FROM user_dead_letter_event AS u 
                WHERE u.is_retryable=:retryable AND u.status = :status AND  u.retry_count<=:maxRetries
                ORDER BY created_date ASC 
                OFFSET :offset LIMIT :limit
                
            """)
    Set<UserErrorEventValueEntity> recoverDeadLetter(@Param("retryable") boolean retryable,@Param("status") String status,
                                                     @Param("offset") int offset,
                                                     @Param("limit") int limit,
                                                     @Param("maxRetries") int maxRetries);
}
