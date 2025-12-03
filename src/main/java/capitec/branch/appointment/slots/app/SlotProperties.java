package capitec.branch.appointment.slots.app;

import capitec.branch.appointment.slots.domain.DayType;
import jakarta.validation.constraints.NotNull;

import java.sql.Time;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record SlotProperties(
        @NotNull LocalTime openTime,
        @NotNull LocalTime closingTime,
        Duration slotDuration,
        int staffCount,
        double utilizationFactor,
        DayType dayType
) {
}


