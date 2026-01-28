package capitec.branch.appointment.branch.infrastructure.controller;

import jakarta.validation.constraints.NotBlank;

public record AddBranchRequest(
        @NotBlank(message = "Branch ID cannot be blank")
        String branchId
) {}
