package capitec.branch.appointment.staffschedular.app;

import capitec.branch.appointment.exeption.EntityAlreadyExistException;
import capitec.branch.appointment.staffschedular.domain.BranchStaffAssignment;
import capitec.branch.appointment.staffschedular.domain.BranchStaffAssignmentService;
import capitec.branch.appointment.staffschedular.domain.StaffRef;
import capitec.branch.appointment.utils.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class SetWeeklyStaffScheduleUseCase {

    private final BranchStaffAssignmentService branchStaffAssignmentService;

    public boolean execute(String branchId, Map<LocalDate, Set<StaffRef>> weeklyStaff) {

        var isAdded = false;

        try {
            var branchStaffAssignment = branchStaffAssignmentService.get(branchId);
            BranchStaffAssignment assignmentToSave;

            if (branchStaffAssignment.isPresent()) {
                assignmentToSave = branchStaffAssignment.get();
                assignmentToSave.setWeeklyStaff(weeklyStaff);
            } else {
                assignmentToSave = new BranchStaffAssignment(branchId, weeklyStaff);
            }

            isAdded = branchStaffAssignmentService.addStaff(assignmentToSave);
        }
        catch (EntityAlreadyExistException e) {

            log.warn("Error, staff already assigned to day (during weekly set)", e);
           // throw new  ResponseStatusException(HttpStatus.CONFLICT, "Staff already assigned to the given day.", e);
        }
        catch (Exception e) {

            log.warn("Error setting weekly working staff schedule for branch {}", branchId, e);
            log.info("Issue event of failed setting weekly staff schedule", e);
        }
        return isAdded;
    }
}