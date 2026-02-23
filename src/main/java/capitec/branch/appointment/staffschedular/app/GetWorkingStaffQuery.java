package capitec.branch.appointment.staffschedular.app;

import capitec.branch.appointment.staffschedular.domain.BranchStaffAssignmentService;
import capitec.branch.appointment.staffschedular.domain.StaffRef;
import capitec.branch.appointment.utils.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Set;

/**
 * Query to get working staff for a branch on a specific date.
 */
@Slf4j
@UseCase
@RequiredArgsConstructor
@Validated
public class GetWorkingStaffQuery {

    private final BranchStaffAssignmentService branchStaffAssignmentService;

    public Set<StaffRef> execute(String branchId, LocalDate date) {
        LocalDate queryDate = date != null ? date : LocalDate.now();

        try {
            return branchStaffAssignmentService.getWorkingStaff(branchId, queryDate);
        } catch (Exception e) {
            log.error("Error getting working staff for branch: {}, date: {}", branchId, queryDate, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }
}

