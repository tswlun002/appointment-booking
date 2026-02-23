package capitec.branch.appointment.staff.infrastructure.controller;

public record StaffCountResponse(
        String branchId,
        int count
) {}
