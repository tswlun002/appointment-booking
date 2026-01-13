package capitec.branch.appointment.slots.app;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
public class SlotGeneratorScheduler {

    private final GenerateSlotsUseCase generateSlotsUseCase;
    private final GetLastestGeneratedSlotDate getLastestGeneratedSlotDate;
    private final TransactionTemplate transactionTemplate;
    private final RetryTemplate slotGenerationRetryTemplate;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Value("${slot.rolling-window-days.init-value:7}")
    private int initRollingWindowDays;

    @Value("${slot.rolling-window-days.daily-value:1}")
    private int dailyRollingWindowDays;

    @Scheduled(cron = "${slot.cron:0 30 0 * * *}", zone = "Africa/Johannesburg")
    public void generateDailySlots() {
        log.info("Starting daily slot generation");
        try {
            executeWithRetry();
            log.info("Completed daily slot generation");
        } catch (Exception e) {
            log.error("Daily slot generation failed after all retries", e);
            // Future: Add alerting here (Slack, email, etc.)
            var slotGenerationFailed = new SlotGenerationSchedulerEventFailure("Slot generation failed  ",
                    LocalDateTime.now(), LocalDate.now().plusDays(1));
            applicationEventPublisher.publishEvent(slotGenerationFailed);
        }
    }

    public void executeWithRetry() {
        slotGenerationRetryTemplate.execute(context -> {
            log.info("Attempt {} of slot generation", context.getRetryCount() + 1);

            transactionTemplate.executeWithoutResult(status -> {
                LocalDate today = LocalDate.now();
                var latestDate = getLastestGeneratedSlotDate.execute(today);

                LocalDate startDate = latestDate
                        .map(date -> date.plusDays(1))
                        .orElse(today.plusDays(1));

                int windowDays = latestDate.isPresent() ? dailyRollingWindowDays : initRollingWindowDays;

                log.info("Generating slots from {} for {} days", startDate, windowDays);
                generateSlotsUseCase.createNext7DaySlots(startDate, windowDays);
            });

            return null;
        });
    }
}