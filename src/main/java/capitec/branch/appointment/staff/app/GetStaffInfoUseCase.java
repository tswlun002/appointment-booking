package capitec.branch.appointment.staff.app;

import capitec.branch.appointment.staff.domain.Staff;
import capitec.branch.appointment.staff.domain.StaffService;
import capitec.branch.appointment.staff.domain.StaffStatus;
import capitec.branch.appointment.utils.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Validated
class GetStaffInfoUseCase {

    private final StaffService staffService;

    private Set<Staff> getStaff(String branchId, StaffStatus status) {
        try {
            return staffService.getStaff(branchId, status);
        } catch (Exception e) {
            log.error("Error getting staff by branch {} and status:{}", branchId, status, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");
        }
    }
    public int getStaffCount(String branchId) {
        return getStaff(branchId, StaffStatus.WORKING).size();
    }

    public Set<String> getStaffUsernames(String branchId, StaffStatus status) {
        return getStaff(branchId, status)
                .stream()
                .map(Staff::username)
                .collect(Collectors.toSet());
    }
}