package capitec.branch.appointment.staff.app;


import capitec.branch.appointment.utils.Username;
import jakarta.validation.constraints.NotBlank;

public record StaffDTO(
        @Username
        String username,
        @NotBlank
        String branchId
) {
}
