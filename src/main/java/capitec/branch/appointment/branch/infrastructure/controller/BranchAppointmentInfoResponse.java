package capitec.branch.appointment.branch.infrastructure.controller;

import capitec.branch.appointment.branch.domain.appointmentinfo.DayType;

import java.time.Duration;

public record BranchAppointmentInfoResponse(
        int staffCount,
        Duration slotDuration,
        double utilizationFactor,
        DayType dayType,
        int maxBookingCapacity
) {}
