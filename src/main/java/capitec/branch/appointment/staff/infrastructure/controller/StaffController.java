package capitec.branch.appointment.staff.infrastructure.controller;

import capitec.branch.appointment.staff.app.AddStaffUseCase;
import capitec.branch.appointment.staff.app.GetStaffInfoUseCase;
import capitec.branch.appointment.staff.app.StaffDTO;
import capitec.branch.appointment.staff.app.UpdateStaffWorkStatusUseCase;
import capitec.branch.appointment.staff.domain.Staff;
import capitec.branch.appointment.staff.domain.StaffStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

/**
 * REST Controller for staff management operations.
 * Provides endpoints to add, update, delete and query staff members.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/staff")
@RequiredArgsConstructor
@Validated
public class StaffController {

    private final AddStaffUseCase addStaffUseCase;
    private final GetStaffInfoUseCase getStaffInfoUseCase;
    private final UpdateStaffWorkStatusUseCase updateStaffWorkStatusUseCase;

    /**
     * Add a new staff member to a branch.
     *
     * @param request the add staff request
     * @param traceId unique trace identifier for request tracking
     * @return success message
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('app_admin')")
    public ResponseEntity<?> addStaff(
            @Valid @RequestBody AddStaffRequest request,
            @RequestHeader("Trace-Id") String traceId
    ) {
        log.info("Adding staff: {} to branch: {}, traceId: {}", request.username(), request.branchId(), traceId);

           addStaffUseCase.execute(new StaffDTO(request.username(),request.branchId()));
           return ResponseEntity.noContent().build();
    }

    /**
     * Get staff members for a branch filtered by status.
     *
     * @param branchId the branch ID
     * @param status   optional staff status filter
     * @param traceId  unique trace identifier for request tracking
     * @return list of staff usernames
     */
    @GetMapping("/branches/{branchId}")
    @PreAuthorize("hasAnyRole('app_staff')")
    public ResponseEntity<StaffListResponse> getStaffByBranch(
            @PathVariable("branchId") String branchId,
            @RequestParam(value = "status", required = false) String status,
            @RequestHeader("Trace-Id") String traceId
    ) {
        log.info("Getting staff for branch: {}, status: {}, traceId: {}", branchId, status, traceId);
        StaffStatus staffStatus = status != null ? StaffStatus.valueOf(status.toUpperCase()) :null;

        Set<String> staffUsernames = getStaffInfoUseCase.getStaffUsernames(branchId, staffStatus);

        log.info("Found {} staff members for branch: {}, traceId: {}", staffUsernames.size(), branchId, traceId);

        return ResponseEntity.ok(new StaffListResponse(staffUsernames, staffUsernames.size()));
    }
    /**
     * Update staff work status.
     *
     * @param username the staff username
     * @param request  the update status request
     * @param traceId  unique trace identifier for request tracking
     * @return updated staff info
     */
    @PatchMapping("/{username}/status")
    @PreAuthorize("hasAnyRole('app_admin')")
    public ResponseEntity<StaffResponse> updateStaffStatus(
            @PathVariable("username") String username,
            @Valid @RequestBody UpdateStaffStatusRequest request,
            @RequestHeader("Trace-Id") String traceId
    ) {
        log.info("Updating staff status: {} to {}, traceId: {}", username, request.status(), traceId);

        StaffStatus newStatus = StaffStatus.valueOf(request.status().toUpperCase());
        Staff execute = updateStaffWorkStatusUseCase.execute(username, newStatus);

        log.info("Staff status updated successfully: {}, traceId: {}", username, traceId);

        return ResponseEntity.ok(toResponse(execute));
    }

    private StaffResponse toResponse(Staff staff) {
        return new StaffResponse(
                staff.username(),
                staff.status().name(),
                staff.branchId()
        );
    }
}
