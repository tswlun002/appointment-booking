package capitec.branch.appointment.event.domain;



import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserDeadLetterService   {
    void saveDeadLetter( UserErrorEvent errorEventValue);
    Set<UserErrorEvent> findByStatus(RecordStatus recordStatus,int offset, int limit );

    void updateStatus( UserErrorEvent errorEventValue);
    List<UserErrorEvent> recoverDeadLetter(boolean isRetryable, DEAD_LETTER_STATUS status, int offset, int limit, int maxRetry);

    Optional<UserErrorEvent> findById(String eventId);
}
