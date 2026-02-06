package capitec.branch.appointment.branch.infrastructure.controller;

import capitec.branch.appointment.branch.app.*;
import capitec.branch.appointment.branch.domain.Branch;
import capitec.branch.appointment.branch.domain.appointmentinfo.BranchAppointmentInfo;
import capitec.branch.appointment.branch.domain.appointmentinfo.DayType;
import capitec.branch.appointment.branch.domain.operationhours.OperationHoursOverride;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * REST Controller for branch management operations.
 * Provides endpoints to add, get, delete branches and manage appointment info and operation hours.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/branches")
@RequiredArgsConstructor
@Validated
public class BranchController {

    private final AddBranchUseCase addBranchUseCase;
    private final GetBranchQuery getBranchQuery;
    private final DeleteBranchUseCase deleteBranchUseCase;
    private final AddBranchAppointmentInfoUseCase addBranchAppointmentInfoUseCase;
    private final AddBranchOperationHourOverride addBranchOperationHourOverride;

    /**
     * Add a new branch.
     *
     * @param request the add branch request
     * @param traceId unique trace identifier for request tracking
     * @return the created branch
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('app_admin')")
    public ResponseEntity<BranchResponse> addBranch(
            @Valid @RequestBody AddBranchRequest request,
            @RequestHeader("Trace-Id") String traceId
    ) {
        log.info("Adding branch: {}, traceId: {}", request.branchId(), traceId);

        BranchDTO dto = new BranchDTO(request.branchId());
        Branch branch = addBranchUseCase.execute(dto);

        log.info("Branch added successfully: {}, traceId: {}", branch.getBranchId(), traceId);

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(branch));
    }

    /**
     * Get a branch by ID.
     *
     * @param branchId the branch ID
     * @param traceId  unique trace identifier for request tracking
     * @return the branch details
     */
    @GetMapping("/{branchId}")
    @PreAuthorize("hasAnyRole('app_admin')")
    public ResponseEntity<BranchResponse> getBranch(
            @PathVariable("branchId") String branchId,
            @RequestHeader("Trace-Id") String traceId
    ) {
        log.info("Getting branch: {}, traceId: {}", branchId, traceId);

        Branch branch = getBranchQuery.execute(branchId);

        log.info("Branch retrieved: {}, traceId: {}", branchId, traceId);

        return ResponseEntity.ok(toResponse(branch));
    }

    /**
     * Get all branches with pagination.
     *
     * @param offset  the offset for pagination
     * @param limit   the limit for pagination
     * @param traceId unique trace identifier for request tracking
     * @return list of branches
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('app_admin')")
    public ResponseEntity<BranchListResponse> getAllBranches(
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            @RequestHeader("Trace-Id") String traceId
    ) {
        log.info("Getting all branches, offset: {}, limit: {}, traceId: {}", offset, limit, traceId);

        Collection<Branch> branches = getBranchQuery.execute(offset, limit);

        List<BranchResponse> branchResponses = branches.stream()
                .map(this::toResponse)
                .toList();

        log.info("Retrieved {} branches, traceId: {}", branchResponses.size(), traceId);

        return ResponseEntity.ok(new BranchListResponse(branchResponses, branchResponses.size()));
    }

    /**
     * Delete a branch.
     *
     * @param branchId the branch ID
     * @param traceId  unique trace identifier for request tracking
     * @return no content
     */
    @DeleteMapping("/{branchId}")
    @PreAuthorize("hasAnyRole('app_admin')")
    public ResponseEntity<Void> deleteBranch(
            @PathVariable("branchId") String branchId,
            @RequestHeader("Trace-Id") String traceId
    ) {
        log.info("Deleting branch: {}, traceId: {}", branchId, traceId);

        deleteBranchUseCase.execute(branchId);

        log.info("Branch deleted: {}, traceId: {}", branchId, traceId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Add or update branch appointment info for a specific day type.
     *
     * @param branchId the branch ID
     * @param dayType  the day type for the appointment info (e.g., MONDAY, TUESDAY, PUBLIC_HOLIDAY)
     * @param request  the appointment info request
     * @param traceId  unique trace identifier for request tracking
     * @return success response
     */
    @PutMapping("/{branchId}/appointment-info/{dayType}")
    @PreAuthorize("hasAnyRole('app_admin')")
    public ResponseEntity<Void> addBranchAppointmentInfo(
            @PathVariable("branchId") String branchId,
            @PathVariable("dayType") DayType dayType,
            @Valid @RequestBody AddBranchAppointmentInfoRequest request,
            @RequestHeader("Trace-Id") String traceId
    ) {
        log.info("Adding/updating appointment info for branch: {}, dayType: {}, traceId: {}",
                branchId, dayType, traceId);

        BranchAppointmentInfoDTO dto = new BranchAppointmentInfoDTO(
                request.staffCount(),
                request.slotDuration(),
                request.utilizationFactor(),
                dayType,
                request.maxBookingCapacity()
        );

        addBranchAppointmentInfoUseCase.execute(branchId, dto);

        log.info("Appointment info added/updated for branch: {}, dayType: {}, traceId: {}", branchId, dayType, traceId);

        return ResponseEntity.ok().build();
    }

    /**
     * Add or update branch operation hours override for a specific date.
     *
     * @param branchId      the branch ID
     * @param effectiveDate the effective date for the override
     * @param request       the operation hours override request
     * @param traceId       unique trace identifier for request tracking
     * @return success response
     */
    @PutMapping("/{branchId}/operation-hours-override/{effectiveDate}")
    @PreAuthorize("hasAnyRole('app_admin')")
    public ResponseEntity<Void> addOperationHoursOverride(
            @PathVariable("branchId") String branchId,
            @PathVariable("effectiveDate") LocalDate effectiveDate,
            @Valid @RequestBody AddOperationHoursOverrideRequest request,
            @RequestHeader("Trace-Id") String traceId
    ) {
        log.info("Adding/updating operation hours override for branch: {}, date: {}, traceId: {}",
                branchId, effectiveDate, traceId);

        BranchOperationHourOverrideDTO dto = new BranchOperationHourOverrideDTO(
                effectiveDate,
                request.openTime(),
                request.closingTime(),
                request.isClosed(),
                request.reason()
        );

        addBranchOperationHourOverride.execute(branchId, dto);

        log.info("Operation hours override added/updated for branch: {}, date: {}, traceId: {}", branchId, effectiveDate, traceId);

        return ResponseEntity.ok().build();
    }

    private BranchResponse toResponse(Branch branch) {
        List<BranchAppointmentInfoResponse> appointmentInfoResponses = Collections.emptyList();
        List<OperationHoursOverrideResponse> operationHoursResponses = Collections.emptyList();

        if (branch.getBranchAppointmentInfo() != null) {
            appointmentInfoResponses = branch.getBranchAppointmentInfo().stream()
                    .map(this::toAppointmentInfoResponse)
                    .toList();
        }

        if (branch.getOperationHoursOverride() != null) {
            operationHoursResponses = branch.getOperationHoursOverride().stream()
                    .map(this::toOperationHoursResponse)
                    .toList();
        }

        return new BranchResponse(
                branch.getBranchId(),
                branch.getBranchName(),
                appointmentInfoResponses,
                operationHoursResponses
        );
    }

    private BranchAppointmentInfoResponse toAppointmentInfoResponse(BranchAppointmentInfo info) {
        return new BranchAppointmentInfoResponse(
                info.staffCount(),
                info.slotDuration(),
                info.utilizationFactor(),
                info.day(),
                info.maxBookingCapacity()
        );
    }

    private OperationHoursOverrideResponse toOperationHoursResponse(OperationHoursOverride override) {
        return new OperationHoursOverrideResponse(
                override.effectiveDate(),
                override.openAt(),
                override.closeAt(),
                override.closed(),
                override.reason()
        );
    }
}
