package capitec.branch.appointment.appointment.infrastructure.controller;

import capitec.branch.appointment.appointment.app.*;
import capitec.branch.appointment.appointment.domain.Appointment;
import capitec.branch.appointment.appointment.domain.AppointmentStatus;
import capitec.branch.appointment.appointment.domain.AttendingAppointmentStateTransitionAction;
import capitec.branch.appointment.appointment.domain.CustomerUpdateAppointmentAction;
import capitec.branch.appointment.sharekernel.Pagination;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
    private final GetAppointmentByIdUseCase getAppointmentByIdUseCase;
    private final GetCustomerAppointmentsUseCase getCustomerAppointmentsUseCase;

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
     * @param offset           offset for pagination (default 0)
     * @param limit            number of results per page (default 50, max 100)
     * @param traceId          unique trace identifier for request tracking
     * @return list of appointments
     */
    @GetMapping("/customer/{customerUsername}")
    public ResponseEntity<AppointmentsResponse> getCustomerAppointments(
            @PathVariable("customerUsername") String customerUsername,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "offset", required = false, defaultValue = "0") int offset,
            @RequestParam(value = "limit", required = false, defaultValue = "50") int limit,
            @RequestHeader("Trace-Id") String traceId
    ) {
        log.info("Getting appointments for customer: {}, status: {}, offset: {}, limit: {}, traceId: {}",
                customerUsername, status, offset, limit, traceId);

        AppointmentStatus statusFilter = status != null ? AppointmentStatus.valueOf(status) : null;
        GetCustomerAppointmentsQuery query = new GetCustomerAppointmentsQuery(customerUsername, statusFilter, offset, limit);

        List<AppointmentWithBranchDTO> appointments = getCustomerAppointmentsUseCase.execute(query);

        List<AppointmentResponse> responses = appointments.stream()
                .map(this::toResponse)
                .toList();

        int totalCount = responses.size();
        log.info("Found {} appointments for customer: {}, traceId: {}",
                totalCount, customerUsername, traceId);

        int totalPages = (int) Math.ceil((double) totalCount / offset);
        boolean hasNext = offset < totalPages - 1;
        boolean hasPrevious = offset > 0;
        boolean isFirstPage = offset == 0;
        boolean isLastPage = offset == totalPages - 1 || totalCount == 0;
        AppointmentsResponse body = new AppointmentsResponse(responses, new Pagination( totalCount, offset, limit, totalPages,
                hasNext, hasPrevious, isFirstPage, isLastPage));
        return ResponseEntity.ok(body);
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

        GetAppointmentByIdQuery query = new GetAppointmentByIdQuery(appointmentId);
        Appointment appointment = getAppointmentByIdUseCase.execute(query);

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

        Appointment appointmentData = getAppointmentByIdUseCase.execute(new GetAppointmentByIdQuery(appointmentId));

        var action = new AttendingAppointmentStateTransitionAction.CheckIn(
                appointmentData.getBranchId(),
                appointmentData.getDateTime().toLocalDate(),
                appointmentData.getCustomerUsername()
        );

        Appointment appointment = attendAppointmentUseCase.execute(action);

        log.info("Check-in successful for appointment: {}, traceId: {}", appointmentId, traceId);

        return ResponseEntity.ok(toResponse(appointment));
    }

    private AppointmentResponse toResponse(AppointmentWithBranchDTO dto) {
        return new AppointmentResponse(
                dto.id(),
                dto.slotId(),
                dto.branchId(),
                dto.branchName(),
                dto.branchAddress(),
                dto.customerUsername(),
                dto.serviceType(),
                dto.status(),
                dto.reference(),
                dto.dateTime(),
                dto.createdAt(),
                dto.updatedAt(),
                dto.checkedInAt(),
                dto.inProgressAt(),
                dto.completedAt(),
                dto.terminatedAt(),
                dto.terminatedBy(),
                dto.terminationReason(),
                dto.terminationNotes(),
                dto.assignedConsultantId(),
                dto.serviceNotes(),
                dto.previousSlotId(),
                dto.rescheduleCount()
        );
    }

    private AppointmentResponse toResponse(Appointment appointment) {
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getSlotId(),
                appointment.getBranchId(),
                null,
                null,
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
