package capitec.branch.appointment.branch.domain;

import capitec.branch.appointment.staff.domain.IsStaffStatus;
import capitec.branch.appointment.utils.Username;

public record StaffRef(
        @Username
        String username,
        @IsStaffStatus(message = "Invalid staff status")
        String status
) {
}
