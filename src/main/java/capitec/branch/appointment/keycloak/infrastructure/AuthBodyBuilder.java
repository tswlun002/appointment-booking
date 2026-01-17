package capitec.branch.appointment.keycloak.infrastructure;

import capitec.branch.appointment.keycloak.domain.AuthBodyType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static capitec.branch.appointment.keycloak.domain.KeycloakConstants.*;

public class AuthBodyBuilder {

    private final String clientId;
    private final String clientSecret;

    public AuthBodyBuilder(String clientId, String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public MultiValueMap<String, String> build(AuthBodyType authBodyType, String adminToken) {
        return switch (authBodyType) {
            case AuthBodyType.LoginAuthBodyType login -> buildLoginBody(login);
            case AuthBodyType.RefreshAuthBodyType refresh -> buildRefreshBody(refresh);
            case AuthBodyType.LogoutAuthBodyType logout -> buildLogoutBody(logout);
            case AuthBodyType.ImpersonateAuthBodyType impersonate -> buildImpersonationBody(impersonate, adminToken);
        };
    }

    private MultiValueMap<String, String> buildLoginBody(AuthBodyType.LoginAuthBodyType login) {
        LinkedMultiValueMap<String, String> params = createBaseParams();
        params.add("scope", DEFAULT_SCOPE);
        params.add("grant_type", GRANT_TYPE_PASSWORD);
        params.add("username", login.username());
        params.add("password", login.password());
        return params;
    }

    private MultiValueMap<String, String> buildRefreshBody(AuthBodyType.RefreshAuthBodyType refresh) {
        LinkedMultiValueMap<String, String> params = createBaseParams();
        params.add("scope", DEFAULT_SCOPE);
        params.add("grant_type", GRANT_TYPE_REFRESH);
        params.add("refresh_token", refresh.refreshToken());
        return params;
    }

    private MultiValueMap<String, String> buildLogoutBody(AuthBodyType.LogoutAuthBodyType logout) {
        LinkedMultiValueMap<String, String> params = createBaseParams();
        params.add("grant_type", GRANT_TYPE_PASSWORD);
        params.add("refresh_token", logout.refreshToken());
        return params;
    }

    private MultiValueMap<String, String> buildImpersonationBody(AuthBodyType.ImpersonateAuthBodyType impersonate, String token) {
        LinkedMultiValueMap<String, String> params = createBaseParams();
        params.add("grant_type", GRANT_TYPE_TOKEN_EXCHANGE);
        params.add("subject_token_type", SUBJECT_TOKEN_TYPE);
        params.add("scope", DEFAULT_SCOPE);
        params.add("subject_token", token);
        params.add("requested_subject", impersonate.username());
        return params;
    }

    private LinkedMultiValueMap<String, String> createBaseParams() {
        LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        return params;
    }
}
