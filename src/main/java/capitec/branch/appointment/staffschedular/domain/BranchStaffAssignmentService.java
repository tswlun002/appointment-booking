package capitec.branch.appointment.staffschedular.domain;

import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

public interface BranchStaffAssignmentService {
    @Transactional
    boolean addStaff(@Valid BranchStaffAssignment branchStaffAssignment);
    Optional<BranchStaffAssignment> get(String branchId);
    Optional<BranchStaffAssignment> get(String branchId, LocalDate date);
    Set<StaffRef> getWorkingStaff(String branchId);
    Set<StaffRef> getWorkingStaff(String branchId, LocalDate date);

    boolean cancelWorkingDay(String branchId, Set<LocalDate> date);
}
