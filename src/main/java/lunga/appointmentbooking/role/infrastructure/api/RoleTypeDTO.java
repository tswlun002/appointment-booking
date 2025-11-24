package lunga.appointmentbooking.role.infrastructure.api;

import lunga.appointmentbooking.utils.GroupName;

public record RoleTypeDTO(
        @GroupName String name
) {
}
