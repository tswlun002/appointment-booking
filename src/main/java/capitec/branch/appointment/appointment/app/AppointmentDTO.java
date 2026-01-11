package capitec.branch.appointment.appointment.app;

import capitec.branch.appointment.utils.Username;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record AppointmentDTO(
        @NotNull(message = "Slot ID cannot be null")
        UUID slotId,

        @NotBlank(message = "Branch ID cannot be blank")
        String branchId,

        @Username
        String customerUsername,

        @NotBlank(message = "Service type cannot be blank")
        String serviceType,

        @NotNull(message = "Appointment day cannot be null")
        LocalDate day,

        @NotNull(message = "Start time cannot be null")
        LocalTime startTime,

        @NotNull(message = "End time cannot be null")
        LocalTime endTime
) {
}
