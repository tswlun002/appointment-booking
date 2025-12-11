package capitec.branch.appointment.staff.app;


import capitec.branch.appointment.staff.domain.StaffStatus;

import java.util.Set;

public interface AvailableStaff {

    int staffCount(String branchId);
    Set<String> getStaff(String branchId, StaffStatus status);
}
