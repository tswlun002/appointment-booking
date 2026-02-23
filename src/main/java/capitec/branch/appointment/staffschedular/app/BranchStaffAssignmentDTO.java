package capitec.branch.appointment.staffschedular.app;

import capitec.branch.appointment.utils.Username;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record BranchStaffAssignmentDTO(
        @Username
        String username,
        @NotNull
        LocalDate day
) {
}
