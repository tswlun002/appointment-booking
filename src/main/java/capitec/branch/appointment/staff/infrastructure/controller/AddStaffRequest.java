package capitec.branch.appointment.staff.infrastructure.controller;

import capitec.branch.appointment.utils.Username;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddStaffRequest(
        @Username
        String username,

        @NotBlank(message = "Branch ID cannot be blank")
        @Size(min = 2, max = 50, message = "Branch ID must be between 2 and 50 characters")
        String branchId
) {}
