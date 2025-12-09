package capitec.branch.appointment.branch.infrastructure;

import capitec.branch.appointment.day.domain.IsDayType;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Duration;
@Table("branch_appointment_info")
public record BranchAppointmentInfoEntity(
        @NotNull
        Duration slotDuration,
        @NotNull
        double utilizationFactor,
        int staffCount,
        @IsDayType(message = "Invalid day type")
        String dayType
) {
}


