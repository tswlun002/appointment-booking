package capitec.branch.appointment.branch.app;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Duration;
import java.time.LocalDate;

public record BranchAppointmentInfoDTO(
        @Min(1)
        int staffCount,
        @NotNull
        Duration slotDuration,
        @NotNull
        double utilizationFactor,
        @NotNull
        LocalDate day,
        @Min(1)
        int maxBookingCapacity
) {
}
