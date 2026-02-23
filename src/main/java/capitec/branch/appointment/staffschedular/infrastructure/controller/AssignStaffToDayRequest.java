package capitec.branch.appointment.staffschedular.infrastructure.controller;

import capitec.branch.appointment.utils.Username;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AssignStaffToDayRequest(
        @Username
        String username,

        @NotNull(message = "Day cannot be null")
        LocalDate day
) {}
