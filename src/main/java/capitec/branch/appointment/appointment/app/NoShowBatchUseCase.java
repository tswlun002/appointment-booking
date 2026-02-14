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
