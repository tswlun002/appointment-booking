package capitec.branch.appointment.user.domain;

public record UserClientDetails(
        String username, String email, String firstname, String lastname,boolean enabled
) {
}
