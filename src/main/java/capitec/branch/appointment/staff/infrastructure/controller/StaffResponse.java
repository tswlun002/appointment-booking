package capitec.branch.appointment.staff.infrastructure.controller;

public record StaffResponse(
        String username,
        String status,
        String branchId
) {}
