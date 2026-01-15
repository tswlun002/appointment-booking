package capitec.branch.appointment.kafka.domain;

import capitec.branch.appointment.event.domain.DEAD_LETTER_STATUS;

import java.util.List;

public interface DeadLetterService< E extends ErrorEventValue> {

    void  saveDeadLetter(E value);
    List<ErrorEventValue> recoverDeadLetter(boolean isRetryable, DEAD_LETTER_STATUS status, int offset, int limit, int maxRetry);
    void updateStatus(E value);

    void markRecovered(E  errorEventValue, Long partition, Long offset);
    void handleRetryFailure(E errorEventValue);
}
