package capitec.branch.appointment.branch.app;

import capitec.branch.appointment.branch.domain.appointmentinfo.DayType;
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
        DayType day,
        @Min(1)
        int maxBookingCapacity
) {
}
