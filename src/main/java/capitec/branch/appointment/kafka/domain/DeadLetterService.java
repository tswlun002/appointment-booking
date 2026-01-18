package capitec.branch.appointment.kafka.domain;

import capitec.branch.appointment.event.domain.DEAD_LETTER_STATUS;

import java.io.Serializable;
import java.util.List;

public interface DeadLetterService<K extends Serializable, V extends Serializable> {

    void  saveDeadLetter(EventValue.EventError<K,V> value);
    List<EventValue.EventError<K,V>> recoverDeadLetter(boolean isRetryable, DEAD_LETTER_STATUS status, int offset, int limit, int maxRetry);
    void updateStatus(EventValue.EventError<K,V> value);

    void markRecovered(EventValue.EventError<K,V> errorEventValue, Long partition, Long offset);
    void handleRetryFailure(EventValue.EventError<K,V> errorEventValue);
}
