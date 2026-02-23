package capitec.branch.appointment.staffschedular.infrastructure.controller;

import capitec.branch.appointment.utils.Username;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

public record SetWeeklyScheduleRequest(
        @NotBlank(message = "Branch ID cannot be blank")
        @Size(min = 2, max = 50, message = "Branch ID must be between 2 and 50 characters")
        String branchId,

        @NotNull(message = "Weekly staff schedule cannot be null")
        @NotEmpty(message = "Weekly staff schedule cannot be empty")
        Map<@NotNull LocalDate, @NotEmpty Set<@Username String>> weeklyStaff
) {}
