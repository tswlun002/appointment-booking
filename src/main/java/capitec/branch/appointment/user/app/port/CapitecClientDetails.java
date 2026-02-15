package capitec.branch.appointment.user.app.port;
/**
 * DTO representing Capitec client details from external system.
 * Part of Anti-Corruption Layer (ACL).
 */
public record CapitecClientDetails(
        String username,
        String email,
        String firstname,
        String lastname,
        boolean enabled
) {
}
