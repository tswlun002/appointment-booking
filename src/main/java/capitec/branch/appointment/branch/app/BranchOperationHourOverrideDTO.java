package capitec.branch.appointment.branch.app;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record BranchOperationHourOverrideDTO(
        @NotNull(message = "Effective date cannot be null")
        LocalDate effectiveDate,
        @NotNull(message = "Open time cannot be null")
        LocalTime openTime,
        @NotNull(message = "Closing time cannot be null")
        LocalTime closingTime,
        @NotNull(message = "Closed flag cannot be null")
        Boolean isClosed,
        @NotNull(message = "Reason cannot be null")
        String reason
) {
}
