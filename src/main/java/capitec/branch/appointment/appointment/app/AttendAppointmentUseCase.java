package capitec.branch.appointment.appointment.app;

import capitec.branch.appointment.appointment.domain.Appointment;
import capitec.branch.appointment.appointment.domain.AppointmentService;
import capitec.branch.appointment.appointment.domain.AppointmentStatus;
import capitec.branch.appointment.appointment.domain.AttendingAppointmentStateTransitionAction;
import capitec.branch.appointment.exeption.OptimisticLockConflictException;
import capitec.branch.appointment.utils.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher publisher;

    @Transactional
    public void execute(AttendingAppointmentStateTransitionAction action) {
        Appointment appointment = resolveAppointment(action);
        AppointmentStatus previousStatus = appointment.getStatus();

        action.execute(appointment, LocalDateTime.now());

        try {
            appointmentService.update(appointment);
        } catch (OptimisticLockConflictException e) {
            log.warn("Concurrent modification detected for appointment: {}", appointment.getId());
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Appointment was modified by another user. Please refresh and try again.", e);
        }

        publishEvent(action, appointment,previousStatus);
    }

    private Appointment resolveAppointment(AttendingAppointmentStateTransitionAction action) {
        return switch (action) {
            case AttendingAppointmentStateTransitionAction.CheckIn(String branchId, LocalDate day, String customerUsername) -> {
                log.info("Check-in for user:{} at branch:{} on date:{}", customerUsername, branchId, day);
                yield appointmentService.getUserActiveAppointment(branchId, day, customerUsername)
                        .orElseThrow(() -> notFound("User:%s at branch:%s on date:%s", customerUsername, branchId, day));
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
        var event = switch (action) {
            case AttendingAppointmentStateTransitionAction.CheckIn(_, _, String customerUsername) ->
                    AppointmentStateChangedEvent.transition(
                            appointment.getId(),
                            appointment.getReference(),
                            customerUsername,
                            previousStatus,
                            AppointmentStatus.CHECKED_IN,
                            customerUsername,
                            Map.of()
                    );
            case AttendingAppointmentStateTransitionAction.StartService(_, String staffUsername) ->
                    AppointmentStateChangedEvent.transition(
                            appointment.getId(),
                            appointment.getReference(),
                            appointment.getCustomerUsername(),
                            previousStatus,
                            AppointmentStatus.IN_PROGRESS,
                            staffUsername,
                            Map.of("staffUsername", staffUsername)
                    );
            case AttendingAppointmentStateTransitionAction.CompleteAttendingAppointment(_, _) ->
                    AppointmentStateChangedEvent.transition(
                            appointment.getId(),
                            appointment.getReference(),
                            appointment.getCustomerUsername(),
                            previousStatus,
                            AppointmentStatus.COMPLETED,
                            "system",
                            Map.of()
                    );
            case AttendingAppointmentStateTransitionAction.CancelByStaff(String staffUsername, String reason, _) ->
                    AppointmentStateChangedEvent.transition(
                            appointment.getId(),
                            appointment.getReference(),
                            appointment.getCustomerUsername(),
                            previousStatus,
                            AppointmentStatus.CANCELLED,
                            staffUsername,
                            Map.of("reason", reason, "staffUsername", staffUsername)
                    );
        };

        log.info("Publishing event: {}", event);
        publisher.publishEvent(event);
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
