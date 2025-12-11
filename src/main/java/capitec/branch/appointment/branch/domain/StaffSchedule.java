package capitec.branch.appointment.branch.domain;

import java.util.Set;

public interface StaffSchedule {

    boolean addWorkingStaff(String branchId, Set<StaffRef> staff);
}
