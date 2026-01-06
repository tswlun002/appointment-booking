package capitec.branch.appointment.staffschedular.app;

import capitec.branch.appointment.exeption.EntityAlreadyExistException;
import capitec.branch.appointment.staffschedular.domain.BranchStaffAssignmentService;
import capitec.branch.appointment.staffschedular.domain.StaffRef;
import capitec.branch.appointment.utils.UseCase;
import jakarta.validation.Valid;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@Slf4j
@UseCase
@Validated
@RequiredArgsConstructor
public class AssignStaffToDayUseCase {

    private final BranchStaffAssignmentService branchStaffAssignmentService;

    public boolean execute(String branchId, @Valid BranchStaffAssignmentDTO assignmentDTO) {

        var day = assignmentDTO.day();
        
        var branchStaffAssignment = branchStaffAssignmentService.get(branchId, day)
                .orElseThrow(() -> {
                    log.error("Could not find branch staff schedular of the given day. Branch:{}, day:{}", branchId, day);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND,"Could not find branch staff schedular of the given day.");
                });

        var staff = new StaffRef(assignmentDTO.username());
        var isAdded = false;

        try {

            branchStaffAssignment.addStaff(day, staff);
            isAdded = branchStaffAssignmentService.addStaff(branchStaffAssignment);
        }
        catch (EntityAlreadyExistException e) {
            log.error("Error, staff:{} already scheduled to work at the given day:{}", assignmentDTO.username(), day, e);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Staff already scheduled to work at the given day.",e);
        }
        catch (Exception e) {
            log.error("Error assigning staff:{} at branch:{} at day:{}", assignmentDTO.username(), branchId, day, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",e);
        }
        return isAdded;
    }
}