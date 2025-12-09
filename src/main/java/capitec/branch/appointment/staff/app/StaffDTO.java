package capitec.branch.appointment.staff.app;


import capitec.branch.appointment.utils.Username;

public record StaffDTO(
        @Username
        String username,
        String branchId
) {
}
