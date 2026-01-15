package capitec.branch.appointment.event.infrastructure.kafka;

import capitec.branch.appointment.event.domain.DEAD_LETTER_STATUS;
import capitec.branch.appointment.event.domain.UserDeadLetterService;
import capitec.branch.appointment.event.domain.UserErrorEvent;
import capitec.branch.appointment.kafka.app.RetryEventPublisherSchedulerUseCase;
import capitec.branch.appointment.kafka.domain.DeadLetterService;
import capitec.branch.appointment.kafka.domain.ErrorEventValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeadLetterImpl implements DeadLetterService<ErrorEventValue> {

    private final UserDeadLetterService userDeadLetterService;

    @Override
    public void saveDeadLetter(ErrorEventValue value) {
        UserErrorEvent event = mapToDomain(value);
        userDeadLetterService.saveDeadLetter(event);
    }

    @Override
    public List<ErrorEventValue> recoverDeadLetter(boolean isRetryable, DEAD_LETTER_STATUS status,
                                                   int offset, int limit, int maxRetry) {
        return userDeadLetterService.recoverDeadLetter(isRetryable, status, offset, limit, maxRetry)
                .stream()
                .map(UserErrorEventMapper::toKafkaErrorEventValue)
                .toList();
    }

    @Override
    public void updateStatus(ErrorEventValue value) {
        UserErrorEvent event = mapToDomain(value);
        userDeadLetterService.updateStatus(event);
    }

    @Override
    public void markRecovered(ErrorEventValue errorEventValue, Long partition, Long offset) {
        UserErrorEvent event = mapToDomain(errorEventValue);
        event.markRecovered(partition, offset);
        userDeadLetterService.updateStatus(event);
    }

    @Override
    public void handleRetryFailure(ErrorEventValue errorEventValue) {
        userDeadLetterService.findById(errorEventValue.getEventId())
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

    private UserErrorEvent mapToDomain(ErrorEventValue value) {
        if (value instanceof UserErrorEventValueImpl port) {
            return UserErrorEventMapper.toKafkaErrorEventValue(port);
        }
        throw new IllegalArgumentException("Unsupported ErrorEventValue type: " + value.getClass());
    }

}
