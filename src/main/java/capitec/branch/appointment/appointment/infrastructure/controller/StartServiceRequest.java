package capitec.branch.appointment.appointment.infrastructure.controller;

import capitec.branch.appointment.utils.Username;

public record StartServiceRequest(
        @Username
        String consultantId
) {}
