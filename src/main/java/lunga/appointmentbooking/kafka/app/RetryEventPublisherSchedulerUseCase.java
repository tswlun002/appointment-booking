package lunga.appointmentbooking.kafka.app;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import lunga.appointmentbooking.kafka.domain.DEAD_LETTER_STATUS;
import lunga.appointmentbooking.kafka.domain.DeadLetterService;
import lunga.appointmentbooking.kafka.domain.ErrorEventValue;
import lunga.appointmentbooking.kafka.domain.RetryEventPublisher;

@Slf4j
@UseCase
@ConditionalOnProperty(value = "kafka.retry-default-impl.enabled", havingValue = "true")
@RequiredArgsConstructor
public class RetryEventPublisherSchedulerUseCase implements RetryEventPublisher {

    private final EventPublishUseCase eventPublishUseCase;

    private final DeadLetterService<ErrorEventValue> deadLetterService;

    @Scheduled(fixedDelayString = "#{schedulerProperties.getFixedRateMS()}")
    @Override
    public void retryDeadLetter() {

        deadLetterService.recoverDeadLetter(true, DEAD_LETTER_STATUS.DEAD)
                .forEach(event ->
                       eventPublishUseCase.publishEventAsync(event)
                                .whenComplete(((results, throwable) -> {

                                    if (throwable != null) {

                                        log.error("Failed to republish dead letter event:{}", event, throwable);
                                    }
                                    else {

                                        log.info("Dead letter event {} successfully republished, traceId:{}", event.getEventId(), event.getTraceId());
                                    }

                                }))
                );

    }
}
