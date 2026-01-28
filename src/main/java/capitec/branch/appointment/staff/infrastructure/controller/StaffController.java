package capitec.branch.appointment.staff.infrastructure.controller;

import capitec.branch.appointment.staff.domain.Staff;
import capitec.branch.appointment.staff.domain.StaffService;
import capitec.branch.appointment.staff.domain.StaffStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;
import java.util.stream.Collectors;

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

    private final StaffService staffService;

    /**
     * Add a new staff member to a branch.
     *
     * @param request the add staff request
     * @param traceId unique trace identifier for request tracking
     * @return success message
     */
    @PostMapping
    public ResponseEntity<StaffResponse> addStaff(
            @Valid @RequestBody AddStaffRequest request,
            @RequestHeader("Trace-Id") String traceId
    ) {
        log.info("Adding staff: {} to branch: {}, traceId: {}", request.username(), request.branchId(), traceId);

        Staff staff = new Staff(request.username(), StaffStatus.TRAINING, request.branchId());

        try {
            boolean added = staffService.addStaff(staff);
            if (!added) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to add staff");
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error adding staff: {}", request.username(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", e);
        }

        log.info("Staff added successfully: {}, traceId: {}", request.username(), traceId);

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(staff));
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
    public ResponseEntity<StaffListResponse> getStaffByBranch(
            @PathVariable("branchId") String branchId,
            @RequestParam(value = "status", required = false) String status,
            @RequestHeader("Trace-Id") String traceId
    ) {
        log.info("Getting staff for branch: {}, status: {}, traceId: {}", branchId, status, traceId);

        StaffStatus staffStatus = status != null ? StaffStatus.valueOf(status.toUpperCase()) : StaffStatus.WORKING;

        Set<Staff> staffSet;
        try {
            staffSet = staffService.getStaff(branchId, staffStatus);
        } catch (Exception e) {
            log.error("Error getting staff for branch: {}", branchId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", e);
        }

        Set<String> usernames = staffSet.stream()
                .map(Staff::username)
                .collect(Collectors.toSet());

        log.info("Found {} staff members for branch: {}, traceId: {}", usernames.size(), branchId, traceId);

        return ResponseEntity.ok(new StaffListResponse(usernames, usernames.size()));
    }

    /**
     * Get working staff count for a branch.
     *
     * @param branchId the branch ID
     * @param traceId  unique trace identifier for request tracking
     * @return staff count
     */
    @GetMapping("/branches/{branchId}/count")
    public ResponseEntity<StaffCountResponse> getStaffCount(
            @PathVariable("branchId") String branchId,
            @RequestHeader("Trace-Id") String traceId
    ) {
        log.info("Getting staff count for branch: {}, traceId: {}", branchId, traceId);

        int count;
        try {
            count = staffService.getStaff(branchId, StaffStatus.WORKING).size();
        } catch (Exception e) {
            log.error("Error getting staff count for branch: {}", branchId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", e);
        }

        log.info("Staff count for branch {}: {}, traceId: {}", branchId, count, traceId);

        return ResponseEntity.ok(new StaffCountResponse(branchId, count));
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
    public ResponseEntity<StaffResponse> updateStaffStatus(
            @PathVariable("username") String username,
            @Valid @RequestBody UpdateStaffStatusRequest request,
            @RequestHeader("Trace-Id") String traceId
    ) {
        log.info("Updating staff status: {} to {}, traceId: {}", username, request.status(), traceId);

        StaffStatus newStatus = StaffStatus.valueOf(request.status().toUpperCase());

        Staff updatedStaff;
        try {
            updatedStaff = staffService.updateStaffWorkStatus(username, newStatus)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff not found"));
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error updating staff status: {}", username, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", e);
        }

        log.info("Staff status updated successfully: {}, traceId: {}", username, traceId);

        return ResponseEntity.ok(toResponse(updatedStaff));
    }

    /**
     * Delete a staff member.
     *
     * @param username the staff username
     * @param traceId  unique trace identifier for request tracking
     * @return no content
     */
    @DeleteMapping("/{username}")
    public ResponseEntity<Void> deleteStaff(
            @PathVariable("username") String username,
            @RequestHeader("Trace-Id") String traceId
    ) {
        log.info("Deleting staff: {}, traceId: {}", username, traceId);

        boolean deleted;
        try {
            deleted = staffService.deleteStaff(username);
        } catch (Exception e) {
            log.error("Error deleting staff: {}", username, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", e);
        }

        if (!deleted) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff not found");
        }

        log.info("Staff deleted successfully: {}, traceId: {}", username, traceId);

        return ResponseEntity.noContent().build();
    }

    private StaffResponse toResponse(Staff staff) {
        return new StaffResponse(
                staff.username(),
                staff.status().name(),
                staff.branchId()
        );
    }
}
