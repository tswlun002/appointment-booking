package capitec.branch.appointment.appointment.infrastructure.controller;

import capitec.branch.appointment.sharekernel.Pagination;

import java.util.List;

public record AppointmentsResponse(
        List<AppointmentResponse> appointments,
        Pagination pagination
) {}
