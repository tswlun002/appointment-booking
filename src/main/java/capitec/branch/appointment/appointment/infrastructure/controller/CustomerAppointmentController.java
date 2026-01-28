package capitec.branch.appointment.appointment.infrastructure.controller;

import capitec.branch.appointment.appointment.app.AppointmentDTO;
import capitec.branch.appointment.appointment.app.AttendAppointmentUseCase;
import capitec.branch.appointment.appointment.app.BookAppointmentUseCase;
import capitec.branch.appointment.appointment.app.CustomerUpdateAppointmentUseCase;
import capitec.branch.appointment.appointment.domain.Appointment;
import capitec.branch.appointment.appointment.domain.AppointmentService;
import capitec.branch.appointment.appointment.domain.AttendingAppointmentStateTransitionAction;
import capitec.branch.appointment.appointment.domain.CustomerUpdateAppointmentAction;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for customer appointment operations.
 * Provides endpoints for booking, cancelling, rescheduling, and checking in to appointments.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
@Validated
public class CustomerAppointmentController {

    private final BookAppointmentUseCase bookAppointmentUseCase;
    private final CustomerUpdateAppointmentUseCase customerUpdateAppointmentUseCase;
    private final AttendAppointmentUseCase attendAppointmentUseCase;
    private final AppointmentService appointmentService;

    /**
     * Book a new appointment.
     *
     * @param request  the appointment booking request
     * @param traceId  unique trace identifier for request tracking
     * @return the created appointment
     */
    @PostMapping("/create")
    public ResponseEntity<AppointmentResponse> createAppointment(
            @Valid @RequestBody CreateAppointmentRequest request,
            @RequestHeader("Trace-Id") String traceId
    ) {
        log.info("Creating appointment for customer: {}, branch: {}, traceId: {}",
                request.customerUsername(), request.branchId(), traceId);

        AppointmentDTO dto = new AppointmentDTO(
                request.slotId(),
                request.branchId(),
                request.customerUsername(),
                request.serviceType(),
                request.day(),
                request.startTime(),
                request.endTime()
        );

        Appointment appointment = bookAppointmentUseCase.execute(dto);

        log.info("Appointment created successfully. Reference: {}, traceId: {}",
                appointment.getReference(), traceId);

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(appointment));
    }

    /**
     * Get appointments for a customer.
     *
     * @param customerUsername the customer username
     * @param status           optional status filter
     * @param traceId          unique trace identifier for request tracking
     * @return list of appointments
     */
    @GetMapping("/customer/{customerUsername}")
    public ResponseEntity<AppointmentsResponse> getCustomerAppointments(
            @PathVariable("customerUsername") String customerUsername,
            @RequestParam(value = "status", required = false) String status,
            @RequestHeader("Trace-Id") String traceId
    ) {
        log.info("Getting appointments for customer: {}, status: {}, traceId: {}",
                customerUsername, status, traceId);

        // TODO: Implement query for customer appointments
        // For now, return empty list - needs implementation of GetCustomerAppointmentsQuery
        return ResponseEntity.ok(new AppointmentsResponse(List.of(), 0));
    }

    /**
     * Get appointment by ID.
     *
     * @param appointmentId the appointment ID
     * @param traceId       unique trace identifier for request tracking
     * @return the appointment details
     */
    @GetMapping("/{appointmentId}")
    public ResponseEntity<AppointmentResponse> getAppointmentById(
            @PathVariable("appointmentId") UUID appointmentId,
            @RequestHeader("Trace-Id") String traceId
    ) {
        log.info("Getting appointment by ID: {}, traceId: {}", appointmentId, traceId);

        Appointment appointment = appointmentService.findById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));

        return ResponseEntity.ok(toResponse(appointment));
    }

    /**
     * Cancel an appointment.
     *
     * @param appointmentId the appointment ID
     * @param request       optional cancellation request with reason
     * @param traceId       unique trace identifier for request tracking
     * @return the cancelled appointment
     */
    @PatchMapping("/{appointmentId}/cancel")
    public ResponseEntity<AppointmentResponse> cancelAppointment(
            @PathVariable("appointmentId") UUID appointmentId,
            @RequestBody(required = false) CancelAppointmentRequest request,
            @RequestHeader("Trace-Id") String traceId
    ) {
        log.info("Cancelling appointment: {}, traceId: {}", appointmentId, traceId);

        String reason = request != null ? request.reason() : null;
        var action = new CustomerUpdateAppointmentAction.Cancel(appointmentId, reason);

        Appointment appointment = customerUpdateAppointmentUseCase.execute(action);

        log.info("Appointment cancelled successfully. Reference: {}, traceId: {}",
                appointment.getReference(), traceId);

        return ResponseEntity.ok(toResponse(appointment));
    }

    /**
     * Reschedule an appointment.
     *
     * @param appointmentId the appointment ID
     * @param request       the reschedule request
     * @param traceId       unique trace identifier for request tracking
     * @return the rescheduled appointment
     */
    @PatchMapping("/{appointmentId}/reschedule")
    public ResponseEntity<AppointmentResponse> rescheduleAppointment(
            @PathVariable("appointmentId") UUID appointmentId,
            @Valid @RequestBody RescheduleAppointmentRequest request,
            @RequestHeader("Trace-Id") String traceId
    ) {
        log.info("Rescheduling appointment: {} to new slot: {}, traceId: {}",
                appointmentId, request.newSlotId(), traceId);

        var action = new CustomerUpdateAppointmentAction.Reschedule(
                appointmentId,
                request.newSlotId(),
                request.newDay().atTime(request.newStartTime()),
                request.newEndTime()
        );

        Appointment appointment = customerUpdateAppointmentUseCase.execute(action);

        log.info("Appointment rescheduled successfully. Reference: {}, traceId: {}",
                appointment.getReference(), traceId);

        return ResponseEntity.ok(toResponse(appointment));
    }

    /**
     * Check in for an appointment.
     *
     * @param appointmentId the appointment ID
     * @param traceId       unique trace identifier for request tracking
     * @return the checked-in appointment
     */
    @PatchMapping("/{appointmentId}/check-in")
    public ResponseEntity<AppointmentResponse> checkInAppointment(
            @PathVariable("appointmentId") UUID appointmentId,
            @RequestHeader("Trace-Id") String traceId
    ) {
        log.info("Check-in for appointment: {}, traceId: {}", appointmentId, traceId);

        Appointment appointment = appointmentService.findById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));

        var action = new AttendingAppointmentStateTransitionAction.CheckIn(
                appointment.getBranchId(),
                appointment.getDateTime().toLocalDate(),
                appointment.getCustomerUsername()
        );

        attendAppointmentUseCase.execute(action);

        // Reload to get updated state
        appointment = appointmentService.findById(appointmentId).orElseThrow();

        log.info("Check-in successful for appointment: {}, traceId: {}", appointmentId, traceId);

        return ResponseEntity.ok(toResponse(appointment));
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
