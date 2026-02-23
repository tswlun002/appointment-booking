package capitec.branch.appointment.appointment.infrastructure.controller;

import capitec.branch.appointment.utils.Username;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record CreateAppointmentRequest(
        @NotNull(message = "Slot ID cannot be null")
        UUID slotId,

        @NotBlank(message = "Branch ID cannot be blank")
        @Size(min = 2, max = 50, message = "Branch ID must be between 2 and 50 characters")
        String branchId,

        @Username
        String customerUsername,

        @NotBlank(message = "Service type cannot be blank")
        @Size(min = 3, max = 100, message = "Service type must be between 3 and 100 characters")
        String serviceType,

        @NotNull(message = "Appointment day cannot be null")
        LocalDate day,

        @NotNull(message = "Start time cannot be null")
        LocalTime startTime,

        @NotNull(message = "End time cannot be null")
        LocalTime endTime
) {}
