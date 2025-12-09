package capitec.branch.appointment.staff.domain;

import jakarta.validation.Valid;

import java.util.Optional;
import java.util.Set;

public interface StaffService {

    boolean addStaff(@Valid Staff staff);
    Optional<Staff> updateStaff(String username, StaffStatus status);
    Set<Staff> getStaffByBranchAndStatus(String branchId, StaffStatus status);


}
