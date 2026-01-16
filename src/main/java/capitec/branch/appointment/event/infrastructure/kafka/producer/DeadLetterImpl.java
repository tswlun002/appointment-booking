package capitec.branch.appointment.event.infrastructure.kafka.producer;

import capitec.branch.appointment.event.domain.DEAD_LETTER_STATUS;
import capitec.branch.appointment.event.domain.ErrorEvent;
import capitec.branch.appointment.event.domain.UserDeadLetterService;
import capitec.branch.appointment.event.infrastructure.kafka.producer.appointment.AppointmentErrorEventMapper;
import capitec.branch.appointment.event.infrastructure.kafka.producer.user.UserErrorEventMapper;
import capitec.branch.appointment.event.infrastructure.kafka.producer.user.UserErrorEventValueImpl;
import capitec.branch.appointment.kafka.app.RetryEventPublisherSchedulerUseCase;
import capitec.branch.appointment.kafka.appointment.AppointmentErrorEventValue;
import capitec.branch.appointment.kafka.domain.DeadLetterService;
import capitec.branch.appointment.kafka.domain.ErrorEventValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.SerializationException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeadLetterImpl implements DeadLetterService<ErrorEventValue> {

    private final UserDeadLetterService userDeadLetterService;

    @Override
    public void saveDeadLetter(ErrorEventValue value) {
        var event = mapToDomain(value);
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
        var event = mapToDomain(value);
        userDeadLetterService.updateStatus(event);
    }

    @Override
    public void markRecovered(ErrorEventValue errorEventValue, Long partition, Long offset) {
        var event = mapToDomain(errorEventValue);
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

    private ErrorEvent mapToDomain(ErrorEventValue value) {

        try {
            if (value instanceof UserErrorEventValueImpl err) {
                return UserErrorEventMapper.toKafkaErrorEventValue(err);
            }
            if(value instanceof AppointmentErrorEventValue err){
                return AppointmentErrorEventMapper.toKafkaErrorEventValue(err);
            }
            throw new IllegalArgumentException("Unsupported ErrorEventValue type: " + value.getClass());
        } catch (JsonProcessingException e) {
                throw new SerializationException("Error converting to ErrorEvent", e);
        }
    }

}
