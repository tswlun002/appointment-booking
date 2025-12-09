package capitec.branch.appointment.staff.domain;


import capitec.branch.appointment.utils.Username;


public record Staff(
        @Username
        String username,
        StaffStatus status,
        String branchId

) {
}
