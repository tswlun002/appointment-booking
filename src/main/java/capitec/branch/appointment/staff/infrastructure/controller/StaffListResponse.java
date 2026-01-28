package capitec.branch.appointment.staff.infrastructure.controller;

import java.util.Set;

public record StaffListResponse(
        Set<String> usernames,
        int totalCount
) {}
