package capitec.branch.appointment.staff.app;

import capitec.branch.appointment.exeption.EntityAlreadyExistException;
import capitec.branch.appointment.staff.domain.Staff;
import capitec.branch.appointment.staff.domain.StaffService;
import capitec.branch.appointment.staff.domain.StaffStatus;
import capitec.branch.appointment.utils.UseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;



@Slf4j
@UseCase
@RequiredArgsConstructor
@Validated
public class AddStaffUseCase {

    private final StaffService staffService;
    private final UserPortService userPortService;

    public boolean execute(@Valid StaffDTO staffDTO) {
        checkUserExists(staffDTO.username());

        var isAdded = false;

        try {

            Staff staff = new Staff(staffDTO.username(), StaffStatus.TRAINING, staffDTO.branchId());
            isAdded = staffService.addStaff(staff);

        } catch (EntityAlreadyExistException e) {

            log.error("Error adding staff, staff already exists in this branch", e);
             throw new  ResponseStatusException(HttpStatus.CONFLICT,"Staff already exists in this branch.",e);
        }
        catch (Exception e) {

            log.error("Error adding staff {}", staffDTO.username(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",e);
        }

        return isAdded;
    }

    void checkUserExists(String username) {
        userPortService.execute(username)
                .orElseThrow(() ->{
                    log.error("Username {} does not exist in this branch", username);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found at company system.");
                });
    }
}