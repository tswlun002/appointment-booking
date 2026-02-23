package capitec.branch.appointment.staffschedular.infrastructure.controller;

import capitec.branch.appointment.staffschedular.app.*;
import capitec.branch.appointment.staffschedular.domain.BranchStaffAssignment;
import capitec.branch.appointment.staffschedular.domain.StaffRef;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
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

    private final AssignStaffToDayUseCase assignStaffToDayUseCase;
    private final SetWeeklyStaffScheduleUseCase setWeeklyStaffScheduleUseCase;
    private final GetBranchScheduleQuery getBranchScheduleQuery;
    private final GetWorkingStaffQuery getWorkingStaffQuery;
    private final CancelFutureWorkingDaysUseCase cancelFutureWorkingDaysUseCase;

    /**
     * Set weekly staff schedule for a branch.
     *
     * @param request the weekly schedule request
     * @param traceId unique trace identifier for request tracking
     * @return success response
     */
    @PostMapping("/weekly")
    @PreAuthorize("hasAnyRole('app_admin')")
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

        setWeeklyStaffScheduleUseCase.execute(request.branchId(), weeklyStaff);

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
    @PreAuthorize("hasAnyRole('app_admin')")
    public ResponseEntity<Void> assignStaffToDay(
            @PathVariable("branchId") String branchId,
            @Valid @RequestBody AssignStaffToDayRequest request,
            @RequestHeader("Trace-Id") String traceId
    ) {
        log.info("Assigning staff: {} to branch: {} on day: {}, traceId: {}",
                request.username(), branchId, request.day(), traceId);

        BranchStaffAssignmentDTO dto = new BranchStaffAssignmentDTO(request.username(), request.day());

        assignStaffToDayUseCase.execute(branchId, dto);

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
    @PreAuthorize("hasAnyRole('app_staff')")
    public ResponseEntity<BranchStaffScheduleResponse> getBranchSchedule(
            @PathVariable("branchId") String branchId,
            @RequestHeader("Trace-Id") String traceId
    ) {
        log.info("Getting schedule for branch: {}, traceId: {}", branchId, traceId);

        BranchStaffAssignment assignment = getBranchScheduleQuery.execute(branchId);

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
    @PreAuthorize("hasAnyRole('app_staff')")
    public ResponseEntity<WorkingStaffResponse> getWorkingStaff(
            @PathVariable("branchId") String branchId,
            @RequestParam(value = "date", required = false) LocalDate date,
            @RequestHeader("Trace-Id") String traceId
    ) {
        LocalDate queryDate = date != null ? date : LocalDate.now();
        log.info("Getting working staff for branch: {}, date: {}, traceId: {}", branchId, queryDate, traceId);

        Set<StaffRef> staffRefs = getWorkingStaffQuery.execute(branchId, queryDate);

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
    @PreAuthorize("hasAnyRole('app_admin')")
    public ResponseEntity<Void> cancelWorkingDays(
            @PathVariable("branchId") String branchId,
            @Valid @RequestBody CancelWorkingDaysRequest request,
            @RequestHeader("Trace-Id") String traceId
    ) {
        log.info("Cancelling working days for branch: {}, days: {}, traceId: {}", branchId, request.days(), traceId);

        DayOfWeek[] daysToCancel = request.days().toArray(new DayOfWeek[0]);
        cancelFutureWorkingDaysUseCase.execute(branchId, daysToCancel);

        log.info("Working days cancelled for branch: {}, traceId: {}", branchId, traceId);

        return ResponseEntity.noContent().build();
    }
}
