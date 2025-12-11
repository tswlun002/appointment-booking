package capitec.branch.appointment.branch.app;

import capitec.branch.appointment.branch.domain.address.Address;

import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record BranchDTO(
        @NotNull
        String branchId,
        @NotNull
        LocalTime openTime,
        @NotNull
        LocalTime closingTime,
        @NotNull
        Address address
) {

}
