package capitec.branch.appointment.kafka.domain;

import java.util.Optional;
import java.util.Set;
public interface DeadLetterService< V extends ErrorEventValue> {

    void  saveDeadLetter(V value);
    Set<ErrorEventValue> recoverDeadLetter(boolean isRetryable, DEAD_LETTER_STATUS status);
    Optional<V> findById(String eventId);
    void updateStatus(V value);
}
