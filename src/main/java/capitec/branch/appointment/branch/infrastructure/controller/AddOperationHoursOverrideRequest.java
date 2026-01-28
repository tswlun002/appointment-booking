package capitec.branch.appointment.branch.infrastructure.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record AddOperationHoursOverrideRequest(

        @NotNull(message = "Open time cannot be null")
        LocalTime openTime,

        @NotNull(message = "Closing time cannot be null")
        LocalTime closingTime,

        @NotNull(message = "Closed flag cannot be null")
        Boolean isClosed,

        @NotBlank(message = "Reason cannot be blank")
        String reason
) {}
