package capitec.branch.appointment.slots.app;

import capitec.branch.appointment.slots.domain.SlotDayType;
import jakarta.validation.constraints.NotNull;

import java.time.Duration;
import java.time.LocalTime;

public record SlotProperties(
        @NotNull LocalTime openTime,
        @NotNull LocalTime closingTime,
        Duration slotDuration,
        int staffCount,
        double utilizationFactor,
        SlotDayType slotDayType,
        int maxBookingCapacity
) {
}


