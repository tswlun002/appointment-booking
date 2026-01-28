package capitec.branch.appointment.branch.infrastructure.controller;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Duration;
import java.time.LocalDate;

public record AddBranchAppointmentInfoRequest(
        @Min(value = 1, message = "Staff count must be at least 1")
        int staffCount,

        @NotNull(message = "Slot duration cannot be null")
        Duration slotDuration,

        @NotNull(message = "Utilization factor cannot be null")
        Double utilizationFactor,

        @NotNull(message = "Day cannot be null")
        LocalDate day,

        @Min(value = 1, message = "Max booking capacity must be at least 1")
        int maxBookingCapacity
) {}
