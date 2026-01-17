package capitec.branch.appointment.keycloak.domain;

public final class KeycloakConstants {
    public static final String GRANT_TYPE_PASSWORD = "password";
    public static final String GRANT_TYPE_REFRESH = "refresh_token";
    public static final String GRANT_TYPE_TOKEN_EXCHANGE = "urn:ietf:params:oauth:grant-type:token-exchange";
    public static final String SUBJECT_TOKEN_TYPE = "urn:ietf:params:oauth:token-type:access_token";
    public static final String DEFAULT_SCOPE = "openid profile email";
    
    private KeycloakConstants() {}
}
