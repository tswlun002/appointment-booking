package capitec.branch.appointment.branch.infrastructure.controller;

import capitec.branch.appointment.sharekernel.Pagination;

import java.util.List;

public record BranchListResponse(
        List<BranchResponse> branches,
        Pagination pagination
) {}
