package capitec.branch.appointment.event.app.port.appointment;

import capitec.branch.appointment.sharekernel.EventTrigger;

import java.time.LocalDateTime;
import java.util.UUID;

public record StaffCanceledAppointmentEvent(
        UUID appointmentId,
        String reference,
        String customerUsername,
        String branchId,
        String previousState,
        String appointmentStatus,
        EventTrigger triggeredBy,
        LocalDateTime createdAt
) {
}
