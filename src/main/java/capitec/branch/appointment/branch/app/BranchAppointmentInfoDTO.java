package capitec.branch.appointment.branch.app;

import capitec.branch.appointment.day.domain.DayType;
import jakarta.validation.constraints.NotNull;

import java.time.Duration;

public record BranchAppointmentInfoDTO(
        @NotNull
        Duration slotDuration,
        @NotNull
        double utilizationFactor,
        @NotNull
        DayType dayType
) {
}
