package capitec.branch.appointment.branch.infrastructure.controller;

import java.util.List;

public record BranchListResponse(
        List<BranchResponse> branches,
        int totalCount
) {}
