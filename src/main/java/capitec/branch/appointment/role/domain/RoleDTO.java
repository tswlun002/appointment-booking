package capitec.branch.appointment.role.domain;

import capitec.branch.appointment.utils.RoleName;
import jakarta.validation.constraints.NotBlank;

public record RoleDTO(
        @RoleName
        String name,
        @NotBlank(message = "role description is required")
        String description
) {
}
