package capitec.branch.appointment.staffschedular.infrastructure.controller;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.util.Set;

public record CancelWorkingDaysRequest(
        @NotNull(message = "Days cannot be null")
        @NotEmpty(message = "Days cannot be empty")
        Set<DayOfWeek> days
) {}
