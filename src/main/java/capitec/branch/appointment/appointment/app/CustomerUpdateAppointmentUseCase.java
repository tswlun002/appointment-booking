package capitec.branch.appointment.appointment.app;

import capitec.branch.appointment.appointment.app.port.UpdateSlotStatePort;
import capitec.branch.appointment.appointment.domain.Appointment;
import capitec.branch.appointment.appointment.domain.AppointmentService;
import capitec.branch.appointment.appointment.domain.AppointmentStatus;
import capitec.branch.appointment.appointment.domain.CustomerUpdateAppointmentAction;
import capitec.branch.appointment.exeption.OptimisticLockConflictException;
import capitec.branch.appointment.utils.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@UseCase
@Slf4j
@RequiredArgsConstructor
@Validated
public class CustomerUpdateAppointmentUseCase {

    private final AppointmentService appointmentService;
    private final UpdateSlotStatePort updateSlotStatePort;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public Appointment execute(CustomerUpdateAppointmentAction action) {

        var appointmentId = action.getId();
        Appointment appointment = appointmentService.findById(appointmentId).orElseThrow(() -> {
                    log.error("Appointment not found. Appointment id {}", appointmentId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found.");
                });
        var previousState = appointment.getStatus();

        try {

            updateSlotStatePort.execute(appointment.getSlotId(), LocalDateTime.now());
            action.execute(appointment, LocalDateTime.now());
            appointment = appointmentService.update(appointment);

        } catch (ResponseStatusException ex) {
            log.error("Appointment slot update failed. Appointment id {}", appointmentId);
            throw ex;
        } catch (OptimisticLockConflictException e) {

            log.error("Failed to update appointment. There was concurrently update. Appointment id {}", appointmentId);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Failed to cancel appointment, there was concurrently update. Please refresh and try again.");
        } catch (Exception e) {
            log.error("Failed to update appointment. Appointment id:{}", appointmentId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error.");
        }

        var event = switch (action) {
            case CustomerUpdateAppointmentAction.Cancel ignored -> new CustomerRescheduledAppointmentEvent(
                    appointment.getId(),
                    appointment.getReference(),
                    appointment.getCustomerUsername(),
                    previousState,
                    AppointmentStatus.RESCHEDULED,
                    appointment.getBranchId()
            );


            case CustomerUpdateAppointmentAction.Reschedule ignored -> new CustomerCanceledAppointmentEvent(
                    appointment.getId(),
                    appointment.getReference(),
                    appointment.getCustomerUsername(),
                    previousState,
                    AppointmentStatus.CANCELLED
            );

        };

        log.info("Customer {} appointment  event:{}.",action.getEventName(), event);
        applicationEventPublisher.publishEvent(event);

        return appointment;

    }
}
