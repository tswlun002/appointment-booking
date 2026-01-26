package capitec.branch.appointment.slots.app.port;

import java.time.Duration;

public record AppointmentInfoDetails(
        Duration slotDuration,
        int staffCount,
        double utilizationFactor,
        int maxBookingCapacity
) {
}
