package lunga.appointmentbooking.user.infrastructure.keycloak;

public record TokenResponse(
        String access_token,
        int expires_in,
        int   refresh_expires_in,
        String token_type,
        String id_token,
        String scope
) {
}
