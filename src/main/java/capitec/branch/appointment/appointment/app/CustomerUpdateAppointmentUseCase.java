package capitec.branch.appointment.appointment.app;

import capitec.branch.appointment.appointment.app.port.UpdateSlotStatePort;
import capitec.branch.appointment.appointment.domain.Appointment;
import capitec.branch.appointment.appointment.domain.AppointmentService;
import capitec.branch.appointment.appointment.domain.AppointmentStatus;
import capitec.branch.appointment.appointment.domain.CustomerUpdateAppointmentAction;
import capitec.branch.appointment.exeption.OptimisticLockConflictException;
import capitec.branch.appointment.appointment.app.dto.CustomerCanceledAppointmentEvent;
import capitec.branch.appointment.appointment.app.dto.CustomerRescheduledAppointmentEvent;
import capitec.branch.appointment.utils.UseCase;
import capitec.branch.appointment.utils.sharekernel.EventTrigger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final AppointmentEventService appointmentEventService;


    @Transactional
    public Appointment execute(CustomerUpdateAppointmentAction action) {

        var appointmentId = action.getId();
        Appointment appointment = appointmentService.findById(appointmentId).orElseThrow(() -> {
                    log.error("Appointment not found. Appointment id {}", appointmentId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found.");
                });
        var previousState = appointment.getStatus();

        try {

            LocalDateTime now = LocalDateTime.now();

            switch (action){
                    case CustomerUpdateAppointmentAction.Reschedule sc -> updateSlotStatePort.reschedule(appointment.getSlotId(),sc.newSlotId(), now);
                    case CustomerUpdateAppointmentAction.Cancel ignored -> updateSlotStatePort.release(appointment.getSlotId(), now);
             }

            action.execute(appointment, now);
            appointment = appointmentService.update(appointment);

        }
        catch (IllegalStateException  | IllegalArgumentException e){
            log.error("Illegal state/argument exception. Appointment id {}", appointmentId, e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(),e);
        }
        catch (ResponseStatusException ex) {
            log.error("Appointment slot update failed. Appointment id {}", appointmentId);
            throw ex;
        } catch (OptimisticLockConflictException e) {

            log.error("Failed to update appointment. There was concurrently update. Appointment id {}", appointmentId);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Failed to cancel appointment, there was concurrently update. Please refresh and try again.",e);
        } catch (Exception e) {
            log.error("Failed to update appointment. Appointment id:{}", appointmentId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error.",e);
        }

        switch (action) {
            case CustomerUpdateAppointmentAction.Reschedule ignored -> {
                var event = new CustomerRescheduledAppointmentEvent(
                        appointment.getId(),
                        appointment.getReference(),
                        appointment.getCustomerUsername(),
                        previousState,
                        AppointmentStatus.RESCHEDULED,
                        appointment.getBranchId(),
                        EventTrigger.CUSTOMER,
                        LocalDateTime.now()

                );
                log.info("Customer {} appointment  event:{}.",action.getEventName(), event);
                appointmentEventService.publishEvent(event);
            }


            case CustomerUpdateAppointmentAction.Cancel ignored ->{
                var event = new CustomerCanceledAppointmentEvent(
                        appointment.getId(),
                        appointment.getReference(),
                        appointment.getCustomerUsername(),
                        appointment.getBranchId(),
                        previousState,
                        AppointmentStatus.CANCELLED,
                        EventTrigger.CUSTOMER,
                        LocalDateTime.now()
                );
                log.info("Customer {} appointment  event:{}.",action.getEventName(), event);
                appointmentEventService.publishEvent(event);
            }


        };



        return appointment;

    }
}
