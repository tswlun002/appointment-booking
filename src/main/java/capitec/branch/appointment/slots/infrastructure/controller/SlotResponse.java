package capitec.branch.appointment.slots.infrastructure.controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record SlotResponse(
        UUID id,
        LocalDate day,
        LocalTime startTime,
        LocalTime endTime,
        int maxBookingCapacity,
        int bookingCount,
        String branchId,
        String status
) {}
