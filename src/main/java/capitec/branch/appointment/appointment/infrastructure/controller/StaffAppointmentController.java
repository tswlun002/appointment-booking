package capitec.branch.appointment.appointment.infrastructure.controller;

import capitec.branch.appointment.appointment.app.AttendAppointmentUseCase;
import capitec.branch.appointment.appointment.domain.Appointment;
import capitec.branch.appointment.appointment.domain.AppointmentService;
import capitec.branch.appointment.appointment.domain.AttendingAppointmentStateTransitionAction;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * REST Controller for staff appointment operations.
 * Provides endpoints for starting service, completing appointments, marking no-shows,
 * and viewing branch appointments.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/staff/appointments")
@RequiredArgsConstructor
@Validated
public class StaffAppointmentController {

    private final AttendAppointmentUseCase attendAppointmentUseCase;
    private final AppointmentService appointmentService;

    /**
     * Start serving a customer.
     *
     * @param appointmentId the appointment ID
     * @param request       the start service request
     * @param traceId       unique trace identifier for request tracking
     * @return the updated appointment
     */
    @PatchMapping("/{appointmentId}/start")
    public ResponseEntity<AppointmentResponse> startAppointmentService(
            @PathVariable("appointmentId") UUID appointmentId,
            @Valid @RequestBody StartServiceRequest request,
            @RequestHeader("Trace-Id") String traceId
    ) {
        log.info("Starting service for appointment: {}, consultant: {}, traceId: {}",
                appointmentId, request.consultantId(), traceId);

        var action = new AttendingAppointmentStateTransitionAction.StartService(
                appointmentId,
                request.consultantId()
        );

        attendAppointmentUseCase.execute(action);

        Appointment appointment = appointmentService.findById(appointmentId).orElseThrow();

        log.info("Service started for appointment: {}, traceId: {}", appointmentId, traceId);

        return ResponseEntity.ok(toResponse(appointment));
    }

    /**
     * Complete an appointment.
     *
     * @param appointmentId the appointment ID
     * @param request       optional completion details
     * @param traceId       unique trace identifier for request tracking
     * @return the completed appointment
     */
    @PatchMapping("/{appointmentId}/complete")
    public ResponseEntity<AppointmentResponse> completeAppointment(
            @PathVariable("appointmentId") UUID appointmentId,
            @RequestBody(required = false) CompleteAppointmentRequest request,
            @RequestHeader("Trace-Id") String traceId
    ) {
        log.info("Completing appointment: {}, traceId: {}", appointmentId, traceId);

        String serviceNotes = request != null ? request.serviceNotes() : null;
        var action = new AttendingAppointmentStateTransitionAction.CompleteAttendingAppointment(
                appointmentId,
                serviceNotes
        );

        attendAppointmentUseCase.execute(action);

        Appointment appointment = appointmentService.findById(appointmentId).orElseThrow();

        log.info("Appointment completed: {}, traceId: {}", appointmentId, traceId);

        return ResponseEntity.ok(toResponse(appointment));
    }

    /**
     * Mark customer as no-show.
     *
     * @param appointmentId the appointment ID
     * @param request       optional no-show details
     * @param traceId       unique trace identifier for request tracking
     * @return the updated appointment
     */
    @PatchMapping("/{appointmentId}/no-show")
    public ResponseEntity<AppointmentResponse> markNoShow(
            @PathVariable("appointmentId") UUID appointmentId,
            @RequestBody(required = false) NoShowRequest request,
            @RequestHeader("Trace-Id") String traceId
    ) {
        log.info("Marking no-show for appointment: {}, traceId: {}", appointmentId, traceId);

        // Get appointment to get staff info - for now use system as canceller
        String notes = request != null ? request.notes() : "Customer did not arrive";
        var action = new AttendingAppointmentStateTransitionAction.CancelByStaff(
                "system", // TODO: Get from security context
                notes,
                appointmentId
        );

        attendAppointmentUseCase.execute(action);

        Appointment appointment = appointmentService.findById(appointmentId).orElseThrow();

        log.info("No-show marked for appointment: {}, traceId: {}", appointmentId, traceId);

        return ResponseEntity.ok(toResponse(appointment));
    }

    /**
     * Get appointments for a branch on a specific date.
     *
     * @param branchId the branch ID
     * @param date     the date to filter appointments
     * @param status   optional status filter
     * @param traceId  unique trace identifier for request tracking
     * @return list of branch appointments
     */
    @GetMapping("/branches/{branchId}")
    public ResponseEntity<AppointmentsResponse> getBranchAppointments(
            @PathVariable("branchId") String branchId,
            @RequestParam("date") LocalDate date,
            @RequestParam(value = "status", required = false) String status,
            @RequestHeader("Trace-Id") String traceId
    ) {
        log.info("Getting appointments for branch: {}, date: {}, status: {}, traceId: {}",
                branchId, date, status, traceId);

        // Using existing method - may need pagination
        var appointments = appointmentService.branchAppointments(branchId, 0, 100);

        // Filter by date
        List<AppointmentResponse> filtered = appointments.stream()
                .filter(a -> a.getDateTime().toLocalDate().equals(date))
                .filter(a -> status == null || a.getStatus().name().equals(status))
                .map(this::toResponse)
                .toList();

        log.info("Found {} appointments for branch: {}, date: {}, traceId: {}",
                filtered.size(), branchId, date, traceId);

        return ResponseEntity.ok(new AppointmentsResponse(filtered, filtered.size()));
    }

    private AppointmentResponse toResponse(Appointment appointment) {
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getSlotId(),
                appointment.getBranchId(),
                appointment.getCustomerUsername(),
                appointment.getServiceType(),
                appointment.getStatus().name(),
                appointment.getReference(),
                appointment.getDateTime(),
                appointment.getCreatedAt(),
                appointment.getUpdatedAt(),
                appointment.getCheckedInAt(),
                appointment.getInProgressAt(),
                appointment.getCompletedAt(),
                appointment.getTerminatedAt(),
                appointment.getTerminatedBy(),
                appointment.getTerminationReason() != null ? appointment.getTerminationReason().name() : null,
                appointment.getTerminationNotes(),
                appointment.getAssignedConsultantId(),
                appointment.getServiceNotes(),
                appointment.getPreviousSlotId(),
                appointment.getRescheduleCount()
        );
    }
}
