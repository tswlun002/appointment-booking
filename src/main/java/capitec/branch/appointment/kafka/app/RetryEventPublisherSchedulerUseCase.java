package capitec.branch.appointment.kafka.app;

import capitec.branch.appointment.kafka.domain.EventValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.Assert;
import capitec.branch.appointment.event.domain.DEAD_LETTER_STATUS;
import capitec.branch.appointment.kafka.domain.DeadLetterService;
import capitec.branch.appointment.kafka.domain.ErrorEventValue;
import capitec.branch.appointment.kafka.domain.RetryEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@UseCase
//@ConditionalOnProperty(value = "kafka.retry-default-impl.enabled", havingValue = "true")
@RequiredArgsConstructor
public class RetryEventPublisherSchedulerUseCase implements RetryEventPublisher {

    private static final int BATCH_SIZE = 50;
    public static final int MAX_RETRY = 5;
    private static final int CIRCUIT_BREAKER_THRESHOLD = 3;

    private final EventPublishUseCase eventPublishUseCase;
    private final DeadLetterService<ErrorEventValue> deadLetterService;

    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicInteger batchFailureCount = new AtomicInteger(0);
    private final AtomicInteger batchSuccessCount = new AtomicInteger(0);


    /**
     * ON DISTRIBUTED SYSTEMS, ENSURE ONLY ONE INSTANCE RUNS THIS SCHEDULED TASK AT A TIME
     * source <a href='https://www.baeldung.com/shedlock-spring'/>
      */
    @Scheduled(fixedDelayString = "#{schedulerProperties.getFixedRateMS()}")
    @Override
    public void retryDeadLetter() {
        if (isCircuitBreakerOpen()) {
            log.warn("Circuit breaker OPEN — {} consecutive failures. Skipping retry cycle.",
                    consecutiveFailures.get());
            attemptCircuitBreakerRecovery();
            return;
        }

        if (!isRunning.compareAndSet(false, true)) {
            log.warn("Previous retry cycle still running, skipping this execution");
            return;
        }

        try {
            processAllBatches();
        } finally {
            isRunning.set(false);
        }
    }

    private void processAllBatches() {
        int offset = 0;
        int processedTotal = 0;
        resetBatchCounters();

        while (true) {
            List<ErrorEventValue> batch = deadLetterService.recoverDeadLetter(
                    true,
                    DEAD_LETTER_STATUS.DEAD,
                    offset,
                    BATCH_SIZE,
                    MAX_RETRY
            );

            if (batch.isEmpty()) {
                log.debug("No more dead letters to process");
                break;
            }

            log.info("Publishing batch of {} dead letters, offset: {}", batch.size(), offset);
            publishBatchAndHandleResults(batch);
            processedTotal += batch.size();

            if (batch.size() < BATCH_SIZE) {
                break;
            }
            offset += BATCH_SIZE;
        }

        evaluateBatchResults(processedTotal);
    }

    private void publishBatchAndHandleResults(List<ErrorEventValue> batch) {
        Assert.notEmpty(batch, "Batch must not be empty");

        var  list = batch.stream().map(s -> (EventValue) s).toList();

        eventPublishUseCase.publishBatchAsync(list)
                .whenComplete((results, batchException) -> {
                    if (batchException != null) {
                        log.error("Entire batch publish failed", batchException);
                        batch.forEach(event -> {
                            batchFailureCount.incrementAndGet();
                            deadLetterService.handleRetryFailure(event);
                        });
                        return;
                    }

                    handleBatchResults(batch, results);
                })
                .join(); // Wait for batch completion before processing next batch
    }

    private void handleBatchResults(List<ErrorEventValue> batch, Map<String, Boolean> results) {
        for (ErrorEventValue event : batch) {
            Boolean success = results.getOrDefault(event.getEventId(), false);

            if (Boolean.TRUE.equals(success)) {
                log.info("Dead letter event {} successfully republished, traceId:{}",
                        event.getEventId(), event.getTraceId());
                batchSuccessCount.incrementAndGet();
                deadLetterService.markRecovered(event, event.getPartition(), event.getOffset());
            } else {
                log.error("Failed to republish dead letter event:{}", event.getEventId());
                batchFailureCount.incrementAndGet();
                deadLetterService.handleRetryFailure(event);
            }
        }

        log.info("Batch complete. Success: {}, Failed: {}",
                batchSuccessCount.get(), batchFailureCount.get());
    }

    private boolean isCircuitBreakerOpen() {
        return consecutiveFailures.get() >= CIRCUIT_BREAKER_THRESHOLD;
    }

    private void attemptCircuitBreakerRecovery() {
        int current = consecutiveFailures.decrementAndGet();
        if (current < CIRCUIT_BREAKER_THRESHOLD) {
            log.info("Circuit breaker HALF-OPEN — will attempt retry on next cycle");
        }
    }

    private void resetBatchCounters() {
        batchFailureCount.set(0);
        batchSuccessCount.set(0);
    }

    private void evaluateBatchResults(int processedTotal) {
        if (processedTotal == 0) return;

        int failures = batchFailureCount.get();
        int successes = batchSuccessCount.get();
        double failureRate = (double) failures / processedTotal;

        if (failureRate >= 0.8) {
            int newFailureCount = consecutiveFailures.incrementAndGet();
            log.warn("High failure rate ({}/{}). Consecutive failure count: {}",
                    failures, processedTotal, newFailureCount);
        } else if (successes > 0) {
            consecutiveFailures.set(0);
            log.info("Retry cycle completed. Processed: {}, Success: {}, Failed: {}",
                    processedTotal, successes, failures);
        }
    }

    public static LocalDateTime calculateNextRetry(int retryCount) {
        long delaySeconds = switch (retryCount) {
            case 1 -> 10;
            case 2 -> 20;
            case 3 -> 40;
            default -> 60;
        };
        return LocalDateTime.now().plusSeconds(delaySeconds);
    }
}
