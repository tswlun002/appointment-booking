package capitec.branch.appointment.staffschedular.infrastructure.controller;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

public record BranchStaffScheduleResponse(
        String branchId,
        Map<LocalDate, Set<String>> weeklyStaff
) {}
