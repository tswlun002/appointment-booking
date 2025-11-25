package capitec.branch.appointment.role.infrastructure.api;

import capitec.branch.appointment.utils.GroupName;

public record RoleTypeDTO(
        @GroupName String name
) {
}
