package capitec.branch.appointment.branch.infrastructure;

import capitec.branch.appointment.day.domain.IsDayType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Duration;
@Table("branch_appointment_info")
record BranchAppointmentInfoEntity(
        @NotBlank
        @Column("branch_business_id")
        String branchId,
        @NotNull
        @Column("slot_duration")
        int slotDuration,
        @NotNull
        @Column("utilization_factor")
        double utilizationFactor,
        @Column("staff_count")
        int staffCount,
        @IsDayType(message = "Invalid day type")
        String dayType
) {
}


