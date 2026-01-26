package capitec.branch.appointment.branch.infrastructure;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;

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
        @NotNull
        LocalDate day,
        @Min(1)
        @Column("max_booking_capacity")
        int maxBookingCapacity
) {
}


