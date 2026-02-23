package capitec.branch.appointment.appointment.infrastructure.controller;

import jakarta.validation.constraints.Size;

public record NoShowRequest(
        @Size(max = 500, message = "Notes cannot exceed 500 characters")
        String notes
) {}
