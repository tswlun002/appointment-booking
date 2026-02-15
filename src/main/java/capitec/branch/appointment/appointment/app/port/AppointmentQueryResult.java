package capitec.branch.appointment.appointment.app.port;

import capitec.branch.appointment.appointment.domain.Appointment;

import java.util.List;

/**
 * Result of paginated appointment query containing data and total count.
 */
public record AppointmentQueryResult(
        List<Appointment> appointments,
        int totalCount
) {
    public static AppointmentQueryResult of(List<Appointment> appointments, int totalCount) {
        return new AppointmentQueryResult(appointments, totalCount);
    }

    public static AppointmentQueryResult empty() {
        return new AppointmentQueryResult(List.of(), 0);
    }
}
