package capitec.branch.appointment.user.infrastructure.keycloak;

public record ServiceAccountRole(
        String id,
        String role,
        String client,
        String clientId,
        String description
) {
}
