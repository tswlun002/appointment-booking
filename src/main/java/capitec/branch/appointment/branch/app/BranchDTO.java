package capitec.branch.appointment.branch.app;

import jakarta.validation.constraints.NotNull;

public record BranchDTO(
        @NotNull
        String branchId
) {

}
