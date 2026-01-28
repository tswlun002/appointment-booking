package capitec.branch.appointment.staffschedular.infrastructure.controller;

import java.time.LocalDate;
import java.util.Set;

public record WorkingStaffResponse(
        String branchId,
        LocalDate date,
        Set<String> staff,
        int totalCount
) {}
