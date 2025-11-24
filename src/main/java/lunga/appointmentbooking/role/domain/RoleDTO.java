package lunga.appointmentbooking.role.domain;

import lunga.appointmentbooking.utils.RoleName;
import jakarta.validation.constraints.NotBlank;

public record RoleDTO(
        @RoleName
        String name,
        @NotBlank(message = "role description is required")
        String description
) {
}
