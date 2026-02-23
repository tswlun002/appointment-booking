package capitec.branch.appointment.staffschedular.infrastructure;

import capitec.branch.appointment.utils.Username;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;

@Table("branch_staff_assignment")
record BranchStaffAssignmentEntity(
        @Id Long id,
        @Username
        String username,
        @NotBlank
        @Column("branch_id")
        String branchId,
        @NotNull
        LocalDate day

) {
}