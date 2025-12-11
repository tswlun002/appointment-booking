package capitec.branch.appointment.branch.infrastructure;

import capitec.branch.appointment.staff.domain.IsStaffStatus;
import capitec.branch.appointment.utils.Username;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Table("branch_staff_assignment")
public record BranchStaffAssignmentEntity(
        @Username
        String username,
        @NotBlank
        @Column("branch_business_id")
        String branchId,
        @NotNull
        LocalDate day,
        @IsStaffStatus(message = "Invalid staff status")
        String status

) {
}