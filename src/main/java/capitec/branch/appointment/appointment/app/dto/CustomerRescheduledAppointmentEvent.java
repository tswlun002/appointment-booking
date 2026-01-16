package capitec.branch.appointment.appointment.app.dto;

import capitec.branch.appointment.utils.sharekernel.EventTrigger;
import capitec.branch.appointment.appointment.domain.AppointmentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record CustomerRescheduledAppointmentEvent(
        UUID appointmentId,
        String reference,
        String customerUsername,
        AppointmentStatus previousState,
        AppointmentStatus appointmentStatus,
        String branchId,
        EventTrigger triggeredBy,
        LocalDateTime createdAt

) {
}
