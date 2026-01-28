package capitec.branch.appointment.staffschedular.infrastructure.controller;

import capitec.branch.appointment.staffschedular.app.AssignStaffToDayUseCase;
import capitec.branch.appointment.staffschedular.app.BranchStaffAssignmentDTO;
import capitec.branch.appointment.staffschedular.app.SetWeeklyStaffScheduleUseCase;
import capitec.branch.appointment.staffschedular.domain.BranchStaffAssignment;
import capitec.branch.appointment.staffschedular.domain.BranchStaffAssignmentService;
import capitec.branch.appointment.staffschedular.domain.StaffRef;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * REST Controller for staff scheduling operations.
 * Provides endpoints to manage staff work schedules at branches.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/staff-schedules")
@RequiredArgsConstructor
@Validated
public class StaffScheduleController {

    private final BranchStaffAssignmentService branchStaffAssignmentService;
    private final AssignStaffToDayUseCase assignStaffToDayUseCase;
    private final SetWeeklyStaffScheduleUseCase setWeeklyStaffScheduleUseCase;

    /**
     * Set weekly staff schedule for a branch.
     *
     * @param request the weekly schedule request
     * @param traceId unique trace identifier for request tracking
     * @return success response
     */
    @PostMapping("/weekly")
    public ResponseEntity<BranchStaffScheduleResponse> setWeeklySchedule(
            @Valid @RequestBody SetWeeklyScheduleRequest request,
            @RequestHeader("Trace-Id") String traceId
    ) {
        log.info("Setting weekly schedule for branch: {}, traceId: {}", request.branchId(), traceId);

        Map<LocalDate, Set<StaffRef>> weeklyStaff = request.weeklyStaff().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .map(StaffRef::new)
                                .collect(Collectors.toSet())
                ));

        boolean success = setWeeklyStaffScheduleUseCase.execute(request.branchId(), weeklyStaff);

        if (!success) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to set weekly schedule");
        }

        log.info("Weekly schedule set successfully for branch: {}, traceId: {}", request.branchId(), traceId);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new BranchStaffScheduleResponse(request.branchId(), request.weeklyStaff())
        );
    }

    /**
     * Assign a staff member to work on a specific day.
     *
     * @param branchId the branch ID
     * @param request  the assignment request
     * @param traceId  unique trace identifier for request tracking
     * @return success response
     */
    @PostMapping("/branches/{branchId}/assign")
    public ResponseEntity<Void> assignStaffToDay(
            @PathVariable("branchId") String branchId,
            @Valid @RequestBody AssignStaffToDayRequest request,
            @RequestHeader("Trace-Id") String traceId
    ) {
        log.info("Assigning staff: {} to branch: {} on day: {}, traceId: {}",
                request.username(), branchId, request.day(), traceId);

        BranchStaffAssignmentDTO dto = new BranchStaffAssignmentDTO(request.username(), request.day());

        boolean success = assignStaffToDayUseCase.execute(branchId, dto);

        if (!success) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to assign staff");
        }

        log.info("Staff assigned successfully: {} to branch: {}, traceId: {}", request.username(), branchId, traceId);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Get staff schedule for a branch.
     *
     * @param branchId the branch ID
     * @param traceId  unique trace identifier for request tracking
     * @return branch staff schedule
     */
    @GetMapping("/branches/{branchId}")
    public ResponseEntity<BranchStaffScheduleResponse> getBranchSchedule(
            @PathVariable("branchId") String branchId,
            @RequestHeader("Trace-Id") String traceId
    ) {
        log.info("Getting schedule for branch: {}, traceId: {}", branchId, traceId);

        BranchStaffAssignment assignment = branchStaffAssignmentService.get(branchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch schedule not found"));

        Map<LocalDate, Set<String>> weeklyStaff = assignment.getWeeklyStaff().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .map(StaffRef::username)
                                .collect(Collectors.toSet())
                ));

        log.info("Found schedule for branch: {}, traceId: {}", branchId, traceId);

        return ResponseEntity.ok(new BranchStaffScheduleResponse(branchId, weeklyStaff));
    }

    /**
     * Get working staff for a branch on a specific date.
     *
     * @param branchId the branch ID
     * @param date     the date to query
     * @param traceId  unique trace identifier for request tracking
     * @return working staff for the date
     */
    @GetMapping("/branches/{branchId}/working")
    public ResponseEntity<WorkingStaffResponse> getWorkingStaff(
            @PathVariable("branchId") String branchId,
            @RequestParam(value = "date", required = false) LocalDate date,
            @RequestHeader("Trace-Id") String traceId
    ) {
        LocalDate queryDate = date != null ? date : LocalDate.now();
        log.info("Getting working staff for branch: {}, date: {}, traceId: {}", branchId, queryDate, traceId);

        Set<StaffRef> staffRefs;
        try {
            staffRefs = branchStaffAssignmentService.getWorkingStaff(branchId, queryDate);
        } catch (Exception e) {
            log.error("Error getting working staff for branch: {}", branchId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", e);
        }

        Set<String> usernames = staffRefs.stream()
                .map(StaffRef::username)
                .collect(Collectors.toSet());

        log.info("Found {} working staff for branch: {}, date: {}, traceId: {}",
                usernames.size(), branchId, queryDate, traceId);

        return ResponseEntity.ok(new WorkingStaffResponse(branchId, queryDate, usernames, usernames.size()));
    }

    /**
     * Cancel working days for a branch.
     *
     * @param branchId the branch ID
     * @param request  the cancel request with days to cancel
     * @param traceId  unique trace identifier for request tracking
     * @return no content
     */
    @DeleteMapping("/branches/{branchId}/days")
    public ResponseEntity<Void> cancelWorkingDays(
            @PathVariable("branchId") String branchId,
            @Valid @RequestBody CancelWorkingDaysRequest request,
            @RequestHeader("Trace-Id") String traceId
    ) {
        log.info("Cancelling working days for branch: {}, days: {}, traceId: {}", branchId, request.days(), traceId);

        LocalDate today = LocalDate.now();
        Set<LocalDate> datesToCancel = request.days().stream()
                .flatMap(dayOfWeek -> {
                    return java.util.stream.IntStream.range(0, 7)
                            .mapToObj(today::plusDays)
                            .filter(d -> d.getDayOfWeek() == dayOfWeek);
                })
                .collect(Collectors.toSet());

        if (datesToCancel.isEmpty()) {
            log.warn("No future dates matching the cancellation request for branch: {}", branchId);
            return ResponseEntity.noContent().build();
        }

        try {
            branchStaffAssignmentService.cancelWorkingDay(branchId, datesToCancel);
        } catch (Exception e) {
            log.error("Error cancelling working days for branch: {}", branchId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", e);
        }

        log.info("Working days cancelled for branch: {}, traceId: {}", branchId, traceId);

        return ResponseEntity.noContent().build();
    }
}
