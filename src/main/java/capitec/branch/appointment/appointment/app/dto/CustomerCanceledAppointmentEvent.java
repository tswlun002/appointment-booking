package capitec.branch.appointment.appointment.app.dto;

import capitec.branch.appointment.utils.sharekernel.EventTrigger;
import capitec.branch.appointment.appointment.domain.AppointmentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record CustomerCanceledAppointmentEvent(
        UUID appointmentId,
        String reference,
        String customerUsername,
        String branchId,
        AppointmentStatus previousState,
        AppointmentStatus appointmentStatus,
        EventTrigger triggeredBy,
        LocalDateTime createdAt
) {
}
