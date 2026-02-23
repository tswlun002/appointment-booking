package capitec.branch.appointment.appointment.app;

import java.util.List;

/**
 * Result of fetching customer appointments with total count for pagination.
 */
public record CustomerAppointmentsResult(
        List<AppointmentWithBranchDTO> appointments,
        int totalCount
) {
    public static CustomerAppointmentsResult of(List<AppointmentWithBranchDTO> appointments, int totalCount) {
        return new CustomerAppointmentsResult(appointments, totalCount);
    }

    public static CustomerAppointmentsResult empty() {
        return new CustomerAppointmentsResult(List.of(), 0);
    }
}
