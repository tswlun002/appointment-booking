package capitec.branch.appointment.event.app.port.appointment;

import capitec.branch.appointment.utils.sharekernel.EventTrigger;

import java.time.LocalDateTime;
import java.util.UUID;

public record CustomerRescheduledAppointmentEvent(
        UUID appointmentId,
        String reference,
        String customerUsername,
        String previousState,
        String appointmentStatus,
        String branchId,
        EventTrigger triggeredBy,
        LocalDateTime createdAt

) {
}
