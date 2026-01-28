package capitec.branch.appointment.branch.infrastructure.controller;

import java.time.LocalDate;
import java.time.LocalTime;

public record OperationHoursOverrideResponse(
        LocalDate effectiveDate,
        LocalTime openAt,
        LocalTime closeAt,
        boolean closed,
        String reason
) {}
