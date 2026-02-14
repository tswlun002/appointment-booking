package capitec.branch.appointment.staff.infrastructure.controller;

import capitec.branch.appointment.sharekernel.Pagination;

import java.util.Set;

public record StaffListResponse(
        Set<String> usernames,
        Pagination pagination
) {}
