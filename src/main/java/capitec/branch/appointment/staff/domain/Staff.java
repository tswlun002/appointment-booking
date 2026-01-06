package capitec.branch.appointment.staff.domain;


import capitec.branch.appointment.utils.Username;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


public record Staff(
        @Username
        String username,
        @NotNull
        StaffStatus status,
        @NotBlank
        String branchId

) {
}
