package capitec.branch.appointment.appointment.infrastructure.controller;

import jakarta.validation.constraints.Size;

public record CompleteAppointmentRequest(
        @Size(max = 1000, message = "Service notes cannot exceed 1000 characters")
        String serviceNotes
) {}
