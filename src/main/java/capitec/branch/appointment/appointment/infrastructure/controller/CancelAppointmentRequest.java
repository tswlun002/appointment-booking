package capitec.branch.appointment.appointment.infrastructure.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelAppointmentRequest(
        @NotBlank(message = "Cancellation reason cannot be blank")
        @Size(min = 2, max = 500, message = "Cancellation reason cannot exceed 500 characters")
        String reason
) {}
