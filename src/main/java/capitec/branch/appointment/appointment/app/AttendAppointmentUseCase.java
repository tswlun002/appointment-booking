package capitec.branch.appointment.appointment.app;

import capitec.branch.appointment.appointment.app.port.AppointmentQueryPort;
import capitec.branch.appointment.appointment.domain.Appointment;
import capitec.branch.appointment.appointment.domain.AppointmentService;
import capitec.branch.appointment.appointment.domain.AppointmentStatus;
import capitec.branch.appointment.appointment.domain.AttendingAppointmentStateTransitionAction;
import capitec.branch.appointment.exeption.OptimisticLockConflictException;
import capitec.branch.appointment.utils.UseCase;
import capitec.branch.appointment.sharekernel.EventTrigger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Use case for managing appointment attendance lifecycle transitions.
 *
 * <p>This use case handles all state transitions that occur when a customer attends their
 * appointment at a branch. It uses a sealed interface pattern ({@link AttendingAppointmentStateTransitionAction})
 * to ensure type-safe handling of different attendance actions.</p>
 *
 * <h2>Supported Actions ({@link AttendingAppointmentStateTransitionAction}):</h2>
 * <ul>
 *   <li><b>CheckIn</b> - Customer checks in at branch (BOOKED → CHECKED_IN)</li>
 *   <li><b>StartService</b> - Staff starts serving the customer (CHECKED_IN → IN_PROGRESS)</li>
 *   <li><b>CompleteAttendingAppointment</b> - Staff completes the appointment (IN_PROGRESS → COMPLETED)</li>
 *   <li><b>CancelByStaff</b> - Staff cancels the appointment with reason (any status → CANCELLED)</li>
 * </ul>
 *
 * <h2>Execution Flow:</h2>
 * <ol>
 *   <li>Resolves the appointment based on the action type:
 *     <ul>
 *       <li>CheckIn: finds by branchId, date, and customerUsername</li>
 *       <li>Other actions: finds by appointmentId</li>
 *     </ul>
 *   </li>
 *   <li>Captures the previous status for event publishing</li>
 *   <li>Executes the state transition on the appointment domain object</li>
 *   <li>Persists the updated appointment (with optimistic locking)</li>
 *   <li>Publishes the appropriate event for downstream consumers (emails, notifications)</li>
 * </ol>
 *
 * <h2>Error Handling:</h2>
 * <ul>
 *   <li><b>NOT_FOUND (404)</b> - Appointment doesn't exist</li>
 *   <li><b>BAD_REQUEST (400)</b> - Invalid state transition (e.g., completing a cancelled appointment)</li>
 *   <li><b>CONFLICT (409)</b> - Optimistic lock conflict (concurrent modification)</li>
 * </ul>
 *
 * <h2>Example Use Cases:</h2>
 *
 * <p><b>1. Customer Check-In:</b></p>
 * <pre>
 * CheckIn action = new CheckIn("470010", LocalDate.now(), "john.doe");
 * // Customer arrives at branch 470010 and checks in for today's appointment
 * </pre>
 *
 * <p><b>2. Staff Starts Service:</b></p>
 * <pre>
 * StartService action = new StartService(appointmentId, "staff.username");
 * // Staff member begins serving the checked-in customer
 * </pre>
 *
 * <p><b>3. Complete Appointment:</b></p>
 * <pre>
 * CompleteAttendingAppointment action = new CompleteAttendingAppointment(appointmentId, "Customer assisted with account query");
 * // Staff marks appointment as completed with notes
 * </pre>
 *
 * <p><b>4. Staff Cancellation:</b></p>
 * <pre>
 * CancelByStaff action = new CancelByStaff("staff.username", "Customer left before service", appointmentId);
 * // Staff cancels appointment with reason
 * </pre>
 *
 * @see AttendingAppointmentStateTransitionAction
 * @see Appointment
 * @see AppointmentStatus
 * @see AppointmentEventService
 */
@UseCase
@Slf4j
@RequiredArgsConstructor
@Validated
public class AttendAppointmentUseCase {

    private final AppointmentService appointmentService;
    private final AppointmentQueryPort appointmentQueryPort;
    private final AppointmentEventService publisher;

    @Transactional
    public Appointment execute(AttendingAppointmentStateTransitionAction action) {
        Appointment appointment = resolveAppointment(action);
        AppointmentStatus previousStatus = appointment.getStatus();

        action.execute(appointment, LocalDateTime.now());

        try {
            appointment = appointmentService.update(appointment);

        }
        catch (IllegalStateException  | IllegalArgumentException e){
            log.error("Illegal state/argument exception. Appointment id {}", appointment.getId(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(),e);
        }
        catch (OptimisticLockConflictException e) {
            log.warn("Concurrent modification detected for appointment: {}", appointment.getId());
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Appointment was modified by another user. Please refresh and try again.", e);
        }

        publishEvent(action, appointment,previousStatus);
        return appointment;
    }

    private Appointment resolveAppointment(AttendingAppointmentStateTransitionAction action) {
        return switch (action) {
            case AttendingAppointmentStateTransitionAction.CheckIn(String branchId, LocalDate day, String customerUsername) -> {
                log.info("Check-in for user:{} at branch:{} on dateOfSlots:{}", customerUsername, branchId, day);
                yield appointmentService.getUserActiveAppointment(branchId, day, customerUsername)
                        .orElseThrow(() -> notFound("User:%s at branch:%s on dateOfSlots:%s", customerUsername, branchId, day));
            }
            case AttendingAppointmentStateTransitionAction.StartService(UUID appointmentId, String staffUsername) -> {
                log.info("Staff {} starting appointment {}", staffUsername, appointmentId);
                yield findById(appointmentId);
            }
            case AttendingAppointmentStateTransitionAction.CompleteAttendingAppointment(UUID appointmentId, _) -> {
                log.info("Completing appointment {}", appointmentId);
                yield findById(appointmentId);
            }
            case AttendingAppointmentStateTransitionAction.CancelByStaff(String staffUsername, _, UUID appointmentId) -> {
                log.info("Cancel appointment:{} by staff:{}", appointmentId, staffUsername);
                yield findById(appointmentId);
            }
        };
    }

    private void publishEvent(AttendingAppointmentStateTransitionAction action, Appointment appointment, AppointmentStatus previousStatus) {
        log.info("Publishing event....");
        switch (action) {
            case AttendingAppointmentStateTransitionAction.CheckIn(_, _, String customerUsername) ->
                    publisher.publishEventChangeStatus(
                            appointment.getId(),
                            appointment.getReference(),
                            customerUsername,
                            appointment.getBranchId(),
                            previousStatus,
                            AppointmentStatus.CHECKED_IN,
                            EventTrigger.CUSTOMER,
                            LocalDateTime.now()

                    );
            case AttendingAppointmentStateTransitionAction.StartService(_, String staffUsername) ->
                    publisher.publishEventChangeStatus(
                            appointment.getId(),
                            appointment.getReference(),
                            appointment.getCustomerUsername(),
                            appointment.getBranchId(),
                            previousStatus,
                            AppointmentStatus.IN_PROGRESS,
                            EventTrigger.STAFF,
                            LocalDateTime.now(),
                            Map.of("staffUsername", staffUsername)
                    );
            case AttendingAppointmentStateTransitionAction.CompleteAttendingAppointment(_, _) ->
                    publisher.publishEventChangeStatus(
                            appointment.getId(),
                            appointment.getReference(),
                            appointment.getCustomerUsername(),
                            appointment.getBranchId(),
                            previousStatus,
                            AppointmentStatus.COMPLETED,
                            EventTrigger.STAFF,
                            LocalDateTime.now()
                    );
            case AttendingAppointmentStateTransitionAction.CancelByStaff(String staffUsername, String reason, _) ->
                    publisher.publishStaffCanceledAppointment(
                            appointment.getId(),
                            appointment.getReference(),
                            appointment.getCustomerUsername(),
                            appointment.getBranchId(),
                            previousStatus,
                            AppointmentStatus.CANCELLED,
                            EventTrigger.STAFF,
                            LocalDateTime.now(),
                            Map.of("reason", reason, "staffUsername", staffUsername)
                    );
        };

    }


    private Appointment findById(UUID appointmentId) {
        return appointmentQueryPort.findById(appointmentId)
                .orElseThrow(() -> notFound("Appointment id:%s", appointmentId));
    }

    private ResponseStatusException notFound(String format, Object... args) {
        String message = String.format(format, args);
        log.error("Appointment not found. {}", message);
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment is not found.");
    }
}
