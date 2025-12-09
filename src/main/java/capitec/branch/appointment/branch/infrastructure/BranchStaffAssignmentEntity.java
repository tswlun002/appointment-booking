package capitec.branch.appointment.branch.infrastructure;

import capitec.branch.appointment.staff.domain.IsStaffStatus;
import capitec.branch.appointment.utils.Username;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;

@Table("branch_staff_assignment")
public record BranchStaffAssignmentEntity(

    @Username
    String username,
    @NotNull
    LocalDate day,
    @IsStaffStatus(message = "Invalid staff status")
    String status
) {}