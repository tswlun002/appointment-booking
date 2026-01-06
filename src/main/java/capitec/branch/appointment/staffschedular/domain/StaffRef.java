package capitec.branch.appointment.staffschedular.domain;

import capitec.branch.appointment.utils.Username;

public record StaffRef(
        @Username
        String username
) {
}
