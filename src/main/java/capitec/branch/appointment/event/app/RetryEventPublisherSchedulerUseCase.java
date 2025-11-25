package capitec.branch.appointment.event.app;

import capitec.branch.appointment.event.domain.UserDeadLetterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import capitec.branch.appointment.kafka.app.UseCase;
import capitec.branch.appointment.kafka.domain.DEAD_LETTER_STATUS;
import capitec.branch.appointment.kafka.domain.RetryEventPublisher;

@UseCase
@Slf4j
@RequiredArgsConstructor
public class RetryEventPublisherSchedulerUseCase implements RetryEventPublisher {

    private final RetryEventPublisherSchedulerService RetryEventPublisherSchedulerService;
    private  final UserDeadLetterService userDeadLetterService;

    @Scheduled(fixedDelayString = "#{schedulerProperties.getFixedRateMS()}")
    @Override
    public void retryDeadLetter() {

        userDeadLetterService.recoverDeadLetter(true, DEAD_LETTER_STATUS.DEAD)
                .parallelStream().forEach(event ->
                        RetryEventPublisherSchedulerService.republishEvent(event)
                                .whenComplete(((results, throwable) -> {

                                    if (throwable == null && results) {

                                        log.info("Dead letter event {} successfully republished, traceId:{}", event.getEventId(), event.getTraceId());
                                    }

                                }))
                );

    }
}