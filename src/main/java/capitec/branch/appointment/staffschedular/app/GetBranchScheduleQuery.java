package capitec.branch.appointment.staffschedular.app;

import capitec.branch.appointment.staffschedular.domain.BranchStaffAssignment;
import capitec.branch.appointment.staffschedular.domain.BranchStaffAssignmentService;
import capitec.branch.appointment.utils.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

/**
 * Query to get staff schedule for a branch.
 */
@Slf4j
@UseCase
@RequiredArgsConstructor
@Validated
public class GetBranchScheduleQuery {

    private final BranchStaffAssignmentService branchStaffAssignmentService;

    public BranchStaffAssignment execute(String branchId) {
        try {
            return branchStaffAssignmentService.get(branchId)
                    .orElseThrow(() -> {
                        log.warn("Branch schedule not found for branch: {}", branchId);
                        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch schedule not found");
                    });
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting branch schedule for branch: {}", branchId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }
}

