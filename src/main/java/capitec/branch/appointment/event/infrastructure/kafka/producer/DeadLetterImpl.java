package capitec.branch.appointment.event.infrastructure.kafka.producer;

import capitec.branch.appointment.event.domain.DEAD_LETTER_STATUS;
import capitec.branch.appointment.event.domain.ErrorEvent;
import capitec.branch.appointment.event.domain.EventDeadLetterService;
import capitec.branch.appointment.kafka.domain.DeadLetterService;
import capitec.branch.appointment.kafka.domain.EventValue;
import capitec.branch.appointment.sharekernel.retry.RetryConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeadLetterImpl<K extends Serializable, V extends Serializable> implements DeadLetterService<K,V> {

    private final EventDeadLetterService eventDeadLetterService;

    @Override
    public void saveDeadLetter(EventValue.EventError<K,V> value) {
        log.info("Saving dead letter event. eventId: {}, topic: {}, traceId: {}",
                value.eventId(), value.topic(), value.traceId());
        var event = mapToDomain(value);
        eventDeadLetterService.saveDeadLetter(event);
        log.debug("Dead letter event saved successfully. eventId: {}", value.eventId());
    }

    @Override
    public List<EventValue.EventError<K,V>> recoverDeadLetter(boolean isRetryable, DEAD_LETTER_STATUS status,
                                                              int offset, int limit, int maxRetry) {
        log.debug("Recovering dead letters. isRetryable: {}, status: {}, offset: {}, limit: {}, maxRetry: {}",
                isRetryable, status, offset, limit, maxRetry);
        var results = eventDeadLetterService.recoverDeadLetter(isRetryable, status, offset, limit, maxRetry)
                .stream()
                .map(ErrorEventMapper::<K, V>toKafka)
                .toList();
        log.debug("Recovered {} dead letter events", results.size());
        return results;
    }

    @Override
    public void updateStatus(EventValue.EventError<K,V> value) {
        log.debug("Updating dead letter status. eventId: {}", value.eventId());
        var event = mapToDomain(value);
        eventDeadLetterService.updateStatus(event);
        log.debug("Dead letter status updated. eventId: {}", value.eventId());
    }

    @Override
    public void markRecovered(EventValue.EventError<K,V> errorEventValue, Long partition, Long offset) {
        log.info("Marking dead letter as recovered. eventId: {}, partition: {}, offset: {}",
                errorEventValue.eventId(), partition, offset);
        var event = mapToDomain(errorEventValue);
        event.markRecovered(partition, offset);
        eventDeadLetterService.updateStatus(event);
        log.info("Dead letter marked as recovered. eventId: {}", errorEventValue.eventId());
    }

    @Override
    public void handleRetryFailure(EventValue.EventError<K,V> errorEventValue) {
        log.debug("Handling retry failure. eventId: {}", errorEventValue.eventId());
        eventDeadLetterService.findById(errorEventValue.eventId())
                .ifPresentOrElse(
                        event -> {
                            event.incrementRetry(RetryConfiguration.MAX_RETRY);

                            if (event.canRetry(RetryConfiguration.MAX_RETRY)) {
                                event.scheduleNextRetry(
                                        RetryConfiguration.calculateNextRetry(event.getRetryCount()));
                                log.info("Scheduled next retry. eventId: {}, retryCount: {}, nextRetryAt: {}",
                                        event.getEventId(), event.getRetryCount(), event.getNextRetryAt());
                            } else {
                                event.markFailed();
                                log.warn("Max retries exceeded, marking as failed. eventId: {}, retryCount: {}",
                                        event.getEventId(), event.getRetryCount());
                            }
                            eventDeadLetterService.updateStatus(event);
                        },
                        () -> log.warn("Dead letter event not found for retry failure handling. eventId: {}",
                                errorEventValue.eventId())
                );
    }

    private ErrorEvent mapToDomain(EventValue.EventError<K,V> value) {
        return ErrorEventMapper.toDomain(value);
    }

}
