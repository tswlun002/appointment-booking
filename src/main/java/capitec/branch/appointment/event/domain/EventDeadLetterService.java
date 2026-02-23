package capitec.branch.appointment.event.domain;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface EventDeadLetterService {
    void saveDeadLetter( ErrorEvent errorEventValue);
    Set<ErrorEvent> findByStatus(RecordStatus recordStatus, int offset, int limit );

    ErrorEvent updateStatus( ErrorEvent errorEventValue);
    List<ErrorEvent> recoverDeadLetter(boolean isRetryable, DEAD_LETTER_STATUS status, int offset, int limit, int maxRetry);

    Optional<ErrorEvent> findById(String eventId);
}
