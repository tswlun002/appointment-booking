package capitec.branch.appointment.branch.app;

import capitec.branch.appointment.branch.domain.address.Address;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record BranchDTO(
        @NotBlank
        String branchId,
        @NotBlank
        LocalTime openTime,
        @NotNull
        LocalTime closingTime,
        @NotNull
        Address address
) {

}
