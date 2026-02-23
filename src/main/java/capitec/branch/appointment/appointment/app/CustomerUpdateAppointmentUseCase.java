package capitec.branch.appointment.appointment.app;

import capitec.branch.appointment.appointment.app.port.AppointmentQueryPort;
import capitec.branch.appointment.appointment.app.port.UpdateSlotStatePort;
import capitec.branch.appointment.appointment.domain.Appointment;
import capitec.branch.appointment.appointment.domain.AppointmentService;
import capitec.branch.appointment.appointment.domain.AppointmentStatus;
import capitec.branch.appointment.appointment.domain.CustomerUpdateAppointmentAction;
import capitec.branch.appointment.exeption.OptimisticLockConflictException;
import capitec.branch.appointment.utils.UseCase;
import capitec.branch.appointment.sharekernel.EventTrigger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

/**
 * Use case for handling customer-initiated appointment updates.
 *
 * <p>This use case allows customers to modify their existing appointments through
 * rescheduling or cancellation. It manages the slot state transitions and publishes
 * appropriate events for downstream notifications.</p>
 *
 * <h2>Supported Actions ({@link CustomerUpdateAppointmentAction}):</h2>
 * <ul>
 *   <li><b>Reschedule</b> - Move appointment to a different slot (releases old slot, reserves new slot)</li>
 *   <li><b>Cancel</b> - Cancel the appointment (releases the slot back to available)</li>
 * </ul>
 *
 * <h2>Execution Flow:</h2>
 * <ol>
 *   <li>Fetches the appointment by ID (throws 404 if not found)</li>
 *   <li>Captures the previous status for event publishing</li>
 *   <li>Updates slot state based on action:
 *     <ul>
 *       <li>Reschedule: releases old slot, reserves new slot</li>
 *       <li>Cancel: releases the slot</li>
 *     </ul>
 *   </li>
 *   <li>Executes the state transition on the appointment domain object</li>
 *   <li>Persists the updated appointment (with optimistic locking)</li>
 *   <li>Publishes the appropriate event (reschedule or cancellation) for email notifications</li>
 * </ol>
 *
 * <h2>Business Rules:</h2>
 * <ul>
 *   <li>Appointment must exist</li>
 *   <li>Appointment must be in a valid state for the action (e.g., cannot cancel already cancelled)</li>
 *   <li>For reschedule: new slot must have available capacity</li>
 * </ul>
 *
 * <h2>Error Handling:</h2>
 * <ul>
 *   <li><b>NOT_FOUND (404)</b> - Appointment doesn't exist</li>
 *   <li><b>BAD_REQUEST (400)</b> - Invalid state transition or illegal argument</li>
 *   <li><b>CONFLICT (409)</b> - Optimistic lock conflict (concurrent modification)</li>
 *   <li><b>INTERNAL_SERVER_ERROR (500)</b> - Unexpected errors</li>
 * </ul>
 *
 * <h2>Example Use Cases:</h2>
 *
 * <p><b>1. Customer Reschedules Appointment:</b></p>
 * <pre>
 * Reschedule action = new Reschedule(appointmentId, newSlotId);
 * // Releases old slot, reserves new slot, updates appointment, sends reschedule email
 * </pre>
 *
 * <p><b>2. Customer Cancels Appointment:</b></p>
 * <pre>
 * Cancel action = new Cancel(appointmentId, "Personal reasons");
 * // Releases slot, marks appointment as CANCELLED, sends cancellation email
 * </pre>
 *
 * @see CustomerUpdateAppointmentAction
 * @see Appointment
 * @see AppointmentStatus
 * @see UpdateSlotStatePort
 * @see AppointmentEventService
 */
@UseCase
@Slf4j
@RequiredArgsConstructor
@Validated
public class CustomerUpdateAppointmentUseCase {

    private final AppointmentService appointmentService;
    private final AppointmentQueryPort appointmentQueryPort;
    private final UpdateSlotStatePort updateSlotStatePort;
    private final AppointmentEventService appointmentEventService;


    @Transactional
    public Appointment execute(CustomerUpdateAppointmentAction action) {

        var appointmentId = action.getId();
        Appointment appointment = appointmentQueryPort.findById(appointmentId).orElseThrow(() -> {
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
            case CustomerUpdateAppointmentAction.Reschedule toReschedule -> {
                log.info("Customer {} appointment  event.",action.getEventName());
                appointmentEventService.publishEventReschedule(
                        appointment.getId(),
                        appointment.getReference(),
                        appointment.getCustomerUsername(),
                        previousState,
                        toReschedule.newStartDateTime().toLocalDate(),
                        toReschedule.newStartDateTime().toLocalTime(),
                        toReschedule.newEndTime(),
                        AppointmentStatus.BOOKED,
                        appointment.getBranchId(),
                        EventTrigger.CUSTOMER,
                        LocalDateTime.now()
                );
            }


            case CustomerUpdateAppointmentAction.Cancel ignored ->{

                log.info("Customer {} appointment  event.",action.getEventName());
                appointmentEventService.publishCustomerCanceledAppointment(
                        appointment.getId(),
                        appointment.getReference(),
                        appointment.getCustomerUsername(),
                        appointment.getBranchId(),
                        previousState,
                        AppointmentStatus.CANCELLED,
                        EventTrigger.CUSTOMER,
                        LocalDateTime.now()
                );
            }
        };

        return appointment;

    }
}
