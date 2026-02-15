package capitec.branch.appointment.appointment.app;

import capitec.branch.appointment.appointment.domain.Appointment;
import capitec.branch.appointment.appointment.domain.AppointmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Scheduled batch use case for marking unattended appointments as NO_SHOW.
 *
 * <p>This use case runs as a scheduled job to identify appointments that were booked
 * but the customer never attended (didn't check in or complete the appointment).
 * It processes appointments from a configurable number of days in the past to avoid
 * marking same-day appointments as no-shows prematurely.</p>
 *
 * <h2>Scheduling:</h2>
 * <ul>
 *   <li><b>Default Schedule:</b> Every hour at :05 minutes, between 6am-6pm (Africa/Johannesburg)</li>
 *   <li><b>Configurable via:</b> {@code appointment.unattended.cron} property</li>
 *   <li><b>Days to look back:</b> Configurable via {@code appointment.unattended.since.in.days} (default: 3 days)</li>
 * </ul>
 *
 * <h2>Execution Flow:</h2>
 * <ol>
 *   <li>Calculates the target date (today minus {@code markNoSinceInDays})</li>
 *   <li>Fetches unattended appointments in batches of 500 using cursor-based pagination</li>
 *   <li>For each batch:
 *     <ul>
 *       <li>Marks each appointment as NO_SHOW</li>
 *       <li>Persists the updates within a transaction</li>
 *       <li>Retries on transient failures</li>
 *     </ul>
 *   </li>
 *   <li>Continues until no more unattended appointments are found</li>
 * </ol>
 *
 * <h2>Unattended Criteria:</h2>
 * <p>An appointment is considered unattended if:</p>
 * <ul>
 *   <li>Appointment date matches the target date (X days ago)</li>
 *   <li>Status is BOOKED or CHECKED_IN (not completed, cancelled, or already no-show)</li>
 * </ul>
 *
 * <h2>Resilience Features:</h2>
 * <ul>
 *   <li><b>Batch Processing:</b> Processes 500 appointments at a time to avoid memory issues</li>
 *   <li><b>Cursor Pagination:</b> Uses last processed ID for efficient pagination</li>
 *   <li><b>Retry Template:</b> Retries transient failures with exponential backoff</li>
 *   <li><b>Transaction per Batch:</b> Each batch is processed in its own transaction</li>
 * </ul>
 *
 * <h2>Distributed Systems Note:</h2>
 * <p>For distributed deployments with multiple instances, use a distributed lock
 * mechanism (e.g., ShedLock) to ensure only one instance runs this scheduled task.</p>
 *
 * <h2>Example Configuration:</h2>
 * <pre>
 * appointment:
 *   unattended:
 *     cron: "0 5 6-18 * * *"  # Every hour at :05, 6am-6pm
 *     since:
 *       in:
 *         days: 3  # Mark appointments from 3 days ago as no-show
 * </pre>
 *
 * @see Appointment#markAsNoShow(LocalDateTime)
 * @see AppointmentService#getUnAttendedAppointments(LocalDate, UUID, int)
 */
@Component
@Slf4j
@Validated
public class NoShowBatchUseCase {

    private static final int BATCH_SIZE = 500;

    private final AppointmentService appointmentService;
    private final TransactionTemplate transactionTemplate;
    private final RetryTemplate retryTemplate;
    private final int markNoSinceInDays;

    public NoShowBatchUseCase(
            AppointmentService appointmentService,
            TransactionTemplate transactionTemplate,
            RetryTemplate retryTemplate,
            @Value("${appointment.unattended.since.in.days:3}")
            int markNoSinceInDays) {
        this.appointmentService = appointmentService;
        this.transactionTemplate = transactionTemplate;
        this.retryTemplate = retryTemplate;
        this.markNoSinceInDays = markNoSinceInDays;
    }

    /**
     * ON DISTRIBUTED SYSTEMS, ENSURE ONLY ONE INSTANCE RUNS THIS SCHEDULED TASK AT A TIME
     * source <a href='https://www.baeldung.com/shedlock-spring'/>
     */
    @Scheduled(cron = "${appointment.unattended.cron:0 5 6-18 * * *}", zone = "Africa/Johannesburg") // Every 1hour 5minutes
    public void processNoShowsForPreviousDay() {
        log.info("Starting no-show batch processing for previous day");

        LocalDate previousDay = LocalDate.now().minusDays(markNoSinceInDays);
        LocalDateTime processingTime = LocalDateTime.now();

        UUID lastProcessedId = null;
        List<Appointment> batch;
        int totalProcessed = 0;

        do {
            final UUID currentLastId = lastProcessedId;

            batch = retryTemplate.execute(context -> {
                if (context.getRetryCount() > 0) {
                    log.warn("Retry attempt {} for no-show batch processing", context.getRetryCount());
                }
                return fetchAndProcessBatch(previousDay, currentLastId, processingTime);
            });

            if (!batch.isEmpty()) {
                lastProcessedId = batch.get(batch.size() - 1).getId();
                totalProcessed += batch.size();
                log.debug("Processed batch of {} appointments, total: {}", batch.size(), totalProcessed);
            }
        } while (!batch.isEmpty());

        log.info("Completed no-show batch processing. Total marked as no-show: {}", totalProcessed);
    }

    protected List<Appointment> fetchAndProcessBatch(
            LocalDate appointmentDate,
            UUID lastProcessedId,
            LocalDateTime processingTime) {

        return transactionTemplate.execute(status -> {
            try {
                Collection<Appointment> candidates = appointmentService.getUnAttendedAppointments(
                        appointmentDate, lastProcessedId, BATCH_SIZE
                );

                if (candidates.isEmpty()) {
                    return Collections.emptyList();
                }

                candidates.forEach(apt -> apt.markAsNoShow(processingTime));
                appointmentService.update(candidates);

                return candidates.stream().toList();
            } catch (Exception e) {
                status.setRollbackOnly();
                log.error("Error processing no-show batch: {}", e.getMessage(), e);
                throw e;
            }
        });
    }
}
