package capitec.branch.appointment.appointment.app;

import capitec.branch.appointment.appointment.domain.Appointment;
import capitec.branch.appointment.appointment.domain.AppointmentService;
import capitec.branch.appointment.appointment.domain.AppointmentStatus;
import capitec.branch.appointment.appointment.domain.AttendingAppointmentStateTransitionAction;
import capitec.branch.appointment.exeption.OptimisticLockConflictException;
import capitec.branch.appointment.utils.UseCase;
import capitec.branch.appointment.utils.sharekernel.EventTrigger;
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

@UseCase
@Slf4j
@RequiredArgsConstructor
@Validated
public class AttendAppointmentUseCase {

    private final AppointmentService appointmentService;
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
        return appointmentService.findById(appointmentId)
                .orElseThrow(() -> notFound("Appointment id:%s", appointmentId));
    }

    private ResponseStatusException notFound(String format, Object... args) {
        String message = String.format(format, args);
        log.error("Appointment not found. {}", message);
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment is not found.");
    }
}
