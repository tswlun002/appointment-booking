package capitec.branch.appointment.appointment.app;

import capitec.branch.appointment.appointment.domain.AppointmentStatus;

import java.util.UUID;

public record CustomerCanceledAppointmentEvent(
        UUID appointmentId,
        String reference,
        String customerUsername,
        AppointmentStatus previousState,
        AppointmentStatus appointmentStatus
) {
}
