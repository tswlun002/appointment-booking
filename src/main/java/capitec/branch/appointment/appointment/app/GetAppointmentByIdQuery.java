package capitec.branch.appointment.appointment.app;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Query to retrieve an appointment by its ID.
 */
public record GetAppointmentByIdQuery(
        @NotNull(message = "Appointment ID is required")
        UUID appointmentId
) {
}
