package capitec.branch.appointment.appointment.infrastructure.controller;

import java.util.List;

public record AppointmentsResponse(
        List<AppointmentResponse> appointments,
        int totalCount
) {}
