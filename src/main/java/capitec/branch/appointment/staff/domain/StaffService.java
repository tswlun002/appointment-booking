package capitec.branch.appointment.staff.domain;

import jakarta.validation.Valid;

import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

public interface StaffService {
    @Transactional
    boolean addStaff(@Valid Staff staff);
    Optional<Staff> updateStaffWorkStatus(String username, StaffStatus status);
    Set<Staff> getStaff(String branchId, StaffStatus status);
    boolean deleteStaff(String username);
}
