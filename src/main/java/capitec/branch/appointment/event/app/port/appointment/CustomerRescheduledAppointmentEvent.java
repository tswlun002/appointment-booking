package capitec.branch.appointment.event.app.port.appointment;

import capitec.branch.appointment.sharekernel.EventTrigger;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public record CustomerRescheduledAppointmentEvent(
        UUID appointmentId,
        String reference,
        String customerUsername,
        String previousState,
        LocalDate day,
        LocalTime startTime,
        LocalTime endTime,
        String appointmentStatus,
        String branchId,
        EventTrigger triggeredBy,
        LocalDateTime createdAt

) {
}
