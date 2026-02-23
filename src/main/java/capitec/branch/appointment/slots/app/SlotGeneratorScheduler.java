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
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

/**
 * Scheduled component for automated daily slot generation.
 *
 * <p>This scheduler runs daily to generate appointment slots for branches.
 * It ensures slots are always available for customers to book by maintaining
 * a rolling window of available dates.</p>
 *
 * <h2>Scheduling:</h2>
 * <ul>
 *   <li><b>Default Schedule:</b> Daily at 00:30 (Africa/Johannesburg timezone)</li>
 *   <li><b>Configurable via:</b> {@code slot.cron} property</li>
 * </ul>
 *
 * <h2>Rolling Window Strategy:</h2>
 * <ul>
 *   <li><b>Initial Run:</b> Generates slots for next {@code initRollingWindowDays} days (default: 7)</li>
 *   <li><b>Daily Run:</b> Generates slots for next {@code dailyRollingWindowDays} days (default: 1)</li>
 *   <li><b>Start Date:</b> Automatically calculated from the latest generated slot date</li>
 * </ul>
 *
 * <h2>Execution Flow:</h2>
 * <ol>
 *   <li>Scheduler triggers at configured cron time</li>
 *   <li>Fetches the latest generated slot date from database</li>
 *   <li>Calculates start date and window size:
 *     <ul>
 *       <li>If no slots exist: starts from tomorrow, generates for {@code initRollingWindowDays}</li>
 *       <li>If slots exist: starts from day after latest, generates for {@code dailyRollingWindowDays}</li>
 *     </ul>
 *   </li>
 *   <li>Delegates to {@link GenerateSlotsUseCase} for actual slot creation</li>
 *   <li>All operations run within a transaction with retry support</li>
 * </ol>
 *
 * <h2>Resilience Features:</h2>
 * <ul>
 *   <li><b>Retry Template:</b> Retries on transient failures with exponential backoff</li>
 *   <li><b>Transaction Template:</b> Ensures atomicity of slot generation</li>
 *   <li><b>Failure Event:</b> Publishes {@link SlotGenerationSchedulerEventFailure} on failure for alerting</li>
 * </ul>
 *
 * <h2>Configuration Properties:</h2>
 * <pre>
 * slot:
 *   cron: "0 30 0 * * *"           # Daily at 00:30
 *   rolling-window-days:
 *     init-value: 7                # Initial run: 7 days ahead
 *     daily-value: 1               # Daily run: 1 day ahead
 * </pre>
 *
 * <h2>Distributed Systems Note:</h2>
 * <p>For deployments with multiple instances, use a distributed lock mechanism
 * (e.g., ShedLock) to ensure only one instance runs this scheduled task.
 * See: <a href="https://www.baeldung.com/shedlock-spring">ShedLock Spring Guide</a></p>
 *
 * @see GenerateSlotsUseCase
 * @see GetLastestGeneratedSlotDate
 * @see SlotGenerationSchedulerEventFailure
 */
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

    /**
     * ON DISTRIBUTED SYSTEMS, ENSURE ONLY ONE INSTANCE RUNS THIS SCHEDULED TASK AT A TIME
     * source <a href='https://www.baeldung.com/shedlock-spring'/>
     */
    @Scheduled(cron = "${slot.cron:0 30 0 * * *}", zone = "Africa/Johannesburg")
    public void execute() {
        log.info("Starting daily slot generation");
        try {
            executeWithRetry();
            log.info("Completed daily slot generation");
        } catch (Exception e) {
            log.error("Daily slot generation failed after all retries", e);
            // Future: Add alerting here (Slack, email, etc.)
            var slotGenerationFailed = new SlotGenerationSchedulerEventFailure("Slot generation failed.",
                    LocalDateTime.now(), LocalDate.now().plusDays(1));
            applicationEventPublisher.publishEvent(slotGenerationFailed);
        }
    }

    public void executeWithRetry() {
        executeWithRetry(Collections.emptySet(),null,null);
    }

    public void executeWithRetry(Set<String> branches, LocalDate fromDate,Integer rollingWindowDays) {

        slotGenerationRetryTemplate.execute(context -> {
            log.info("Attempt {} of slot generation", context.getRetryCount() + 1);

            transactionTemplate.executeWithoutResult(status -> {
                try {
                    LocalDate today =  LocalDate.now();
                    LocalDate startDate;
                    int windowDays;
                    if(fromDate == null) {
                        var latestDate = getLastestGeneratedSlotDate.execute(today);
                         windowDays = latestDate.isPresent() ? dailyRollingWindowDays : initRollingWindowDays;

                        startDate = latestDate
                                .map(date -> date.plusDays(1))
                                .orElse(today.plusDays(1));

                    }
                    else{

                        startDate = fromDate;
                        windowDays = rollingWindowDays;
                    }


                    log.info("Generating slots from {} for {} days", startDate, windowDays);
                    generateSlotsUseCase.createNext7DaySlots(branches,startDate, windowDays);

                }catch (Exception e) {
                    log.error("Failed to generate slot from day\n",e );
                    throw e;
                }
            });

            return null;
        });
    }
}