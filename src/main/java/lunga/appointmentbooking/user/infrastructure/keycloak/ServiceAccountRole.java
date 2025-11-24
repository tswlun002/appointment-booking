package lunga.appointmentbooking.user.infrastructure.keycloak;

public record ServiceAccountRole(
        String id,
        String role,
        String client,
        String clientId,
        String description
) {
}
