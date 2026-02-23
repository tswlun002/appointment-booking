package capitec.branch.appointment.staff.infrastructure.controller;

import capitec.branch.appointment.staff.domain.IsStaffStatus;
import jakarta.validation.constraints.NotBlank;

public record UpdateStaffStatusRequest(
        @NotBlank(message = "Status cannot be blank")
        @IsStaffStatus(message = "Invalid staff status. Valid values are: TRAINING, WORKING, LEAVE")
        String status
) {}
