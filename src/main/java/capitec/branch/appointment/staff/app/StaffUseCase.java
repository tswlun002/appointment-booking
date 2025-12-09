package capitec.branch.appointment.staff.app;

import capitec.branch.appointment.staff.domain.Staff;
import capitec.branch.appointment.staff.domain.StaffService;
import capitec.branch.appointment.staff.domain.StaffStatus;
import capitec.branch.appointment.utils.UseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Validated
public class StaffUseCase implements AvailableStaff {

    private final StaffService staffService;


    boolean addStaff(@Valid StaffDTO staffDTO) {
        var isAdded = false;

        try {

            Staff staff = new Staff(staffDTO.username(), StaffStatus.TRAINING,staffDTO.branchId());

          isAdded=  staffService.addStaff(staff);

        } catch (Exception e) {
            log.error( "Error adding staff {}", staffDTO.username(), e );
            throw  new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");
        }

        return isAdded;
    }

    Staff updateStaff(String username , StaffStatus status) {

        Optional<Staff> isUpdated;
        try {

            isUpdated= staffService.updateStaff(username, status);

        }catch (Exception e) {

            log.error( "Error updating staff {}", username, e );

            throw  new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");

        }

        return isUpdated.orElseThrow(()->{

            log.error( "Staff with username:{} is not found ", username );
            return  new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff not found");
        });
    }

    Set<Staff> getStaffByBranchIdAndStatus(String branchId, StaffStatus status) {
        try{

            return  staffService.getStaffByBranchAndStatus(branchId,status);

        } catch (Exception e) {
            log.error( "Error getting staff by branch {} and status:{}", branchId, status, e );
            throw new  ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");
        }
    }

    @Override
    public int staffCount(String branchId) {
        return  getStaffByBranchIdAndStatus(branchId, StaffStatus.WORKING).size();
    }

}
