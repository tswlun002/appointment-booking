package capitec.branch.appointment.staffschedular.app;

import capitec.branch.appointment.exeption.EntityAlreadyExistException;
import capitec.branch.appointment.staffschedular.domain.BranchStaffAssignment;
import capitec.branch.appointment.staffschedular.domain.BranchStaffAssignmentService;
import capitec.branch.appointment.staffschedular.domain.StaffRef;
import capitec.branch.appointment.utils.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class SetWeeklyStaffScheduleUseCase {

    private final BranchStaffAssignmentService branchStaffAssignmentService;

    public void execute(String branchId, Map<LocalDate, Set<StaffRef>> weeklyStaff) {
        try {
            var branchStaffAssignment = branchStaffAssignmentService.get(branchId);
            BranchStaffAssignment assignmentToSave;

            if (branchStaffAssignment.isPresent()) {
                assignmentToSave = branchStaffAssignment.get();
                assignmentToSave.setWeeklyStaff(weeklyStaff);
            } else {
                assignmentToSave = new BranchStaffAssignment(branchId, weeklyStaff);
            }

            boolean success = branchStaffAssignmentService.addStaff(assignmentToSave);
            if (!success) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to set weekly schedule");
            }
        } catch (EntityAlreadyExistException e) {
            log.warn("Staff already assigned to day (during weekly set)", e);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Staff already assigned to the given day", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error setting weekly working staff schedule for branch {}", branchId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to set weekly schedule", e);
        }
    }
}
