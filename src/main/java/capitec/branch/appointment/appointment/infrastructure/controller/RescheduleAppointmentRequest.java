package capitec.branch.appointment.appointment.infrastructure.controller;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record RescheduleAppointmentRequest(
        @NotNull(message = "New slot ID cannot be null")
        UUID newSlotId,

        @NotNull(message = "New day cannot be null")
        LocalDate newDay,

        @NotNull(message = "New start time cannot be null")
        LocalTime newStartTime,

        @NotNull(message = "New end time cannot be null")
        LocalTime newEndTime
) {}
