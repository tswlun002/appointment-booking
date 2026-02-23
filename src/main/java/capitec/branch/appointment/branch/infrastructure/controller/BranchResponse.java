package capitec.branch.appointment.branch.infrastructure.controller;

import java.util.List;

public record BranchResponse(
        String branchId,
        String branchName,
        List<BranchAppointmentInfoResponse> appointmentInfo,
        List<OperationHoursOverrideResponse> operationHoursOverrides
) {}
