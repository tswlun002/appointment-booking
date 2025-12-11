package capitec.branch.appointment.branch.app;

import capitec.branch.appointment.day.domain.DayType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Duration;

public record BranchAppointmentInfoDTO(
        @Min(1)
        int staffCount,
        @NotNull
        Duration slotDuration,
        @NotNull
        double utilizationFactor,
        @NotNull
        DayType dayType
) {
}
