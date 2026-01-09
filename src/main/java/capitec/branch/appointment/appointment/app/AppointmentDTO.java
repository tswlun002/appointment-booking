package capitec.branch.appointment.appointment.app;

import capitec.branch.appointment.utils.Username;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AppointmentDTO(
        @NotNull(message = "Slot ID cannot be null")
        UUID slotId,
        @NotBlank(message = "Branch ID cannot be blank")
        String branchId,
        @Username
        String customerUsername,
        @NotBlank(message = "Service type cannot be blank")
        String serviceType
){
}
