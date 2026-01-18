package capitec.branch.appointment.event.infrastructure.kafka.producer;

import capitec.branch.appointment.event.domain.DEAD_LETTER_STATUS;
import capitec.branch.appointment.event.domain.ErrorEvent;
import capitec.branch.appointment.event.domain.UserDeadLetterService;
import capitec.branch.appointment.kafka.app.RetryEventPublisherSchedulerUseCase;
import capitec.branch.appointment.kafka.domain.DeadLetterService;
import capitec.branch.appointment.kafka.domain.EventValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeadLetterImpl<K extends Serializable, V extends Serializable> implements DeadLetterService<K,V> {

    private final UserDeadLetterService userDeadLetterService;

    @Override
    public void saveDeadLetter(EventValue.EventError<K,V> value) {
        var event = mapToDomain(value);
        userDeadLetterService.saveDeadLetter(event);
    }

    @Override
    public List<EventValue.EventError<K,V>> recoverDeadLetter(boolean isRetryable, DEAD_LETTER_STATUS status,
                                                              int offset, int limit, int maxRetry) {
       return
                userDeadLetterService.recoverDeadLetter(isRetryable, status, offset, limit, maxRetry)
                .stream()
                .map(ErrorEventMapper::<K, V>toKafka)
                .toList();

    }

    @Override
    public void updateStatus(EventValue.EventError<K,V> value) {
        var event = mapToDomain(value);
        userDeadLetterService.updateStatus(event);
    }

    @Override
    public void markRecovered(EventValue.EventError<K,V> errorEventValue, Long partition, Long offset) {
        var event = mapToDomain(errorEventValue);
        event.markRecovered(partition, offset);
        userDeadLetterService.updateStatus(event);
    }

    @Override
    public void handleRetryFailure(EventValue.EventError<K,V> errorEventValue) {
        userDeadLetterService.findById(errorEventValue.eventId())
                .ifPresent(event -> {
                    int maxRetry = RetryEventPublisherSchedulerUseCase.MAX_RETRY;
                    event.incrementRetry(maxRetry);

                    if (event.canRetry(maxRetry)) {
                        event.scheduleNextRetry(
                                RetryEventPublisherSchedulerUseCase.calculateNextRetry(event.getRetryCount()));
                    } else {
                        event.markFailed();
                    }
                    userDeadLetterService.updateStatus(event);
                });
    }

    private ErrorEvent mapToDomain(EventValue.EventError<K,V> value) {

            return ErrorEventMapper.toDomain(value);

    }

}
