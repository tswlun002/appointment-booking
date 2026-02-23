package capitec.branch.appointment.keycloak.infrastructure;

import capitec.branch.appointment.keycloak.domain.AuthBodyType;
import capitec.branch.appointment.keycloak.domain.KeycloakService;
import capitec.branch.appointment.user.domain.User;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.*;
import org.keycloak.admin.client.token.TokenManager;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.ClientRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static capitec.branch.appointment.utils.KeycloakUtils.keyCloakRequest;

@Slf4j
@Service
public class KeycloakServiceImpl implements KeycloakService {

    private static final String VERIFY_USER_CREDENTIALS_URL = "/realms/%s/verify/password";
    private static final String TOKEN_URL = "/realms/%s/protocol/openid-connect/token";
    private static final String LOGOUT_URL = "/realms/%s/protocol/openid-connect/logout";

    private final Keycloak keycloak;
    private final KeycloakRestClient keycloakRestClient;
    private final AuthBodyBuilder authBodyBuilder;
    private final String realm;
    private final String adminClientId;

    public KeycloakServiceImpl(
            RestClient restClient,
            Keycloak keycloak,
            @Value("${keycloak.realm}") String realm,
            @Value("${keycloak.adminClientId}") String adminClientId,
            @Value("${keycloak.adminClientSecret}") String adminClientSecret) {
        this.keycloak = keycloak;
        this.realm = realm;
        this.adminClientId = adminClientId;
        this.keycloakRestClient = new KeycloakRestClient(restClient);
        this.authBodyBuilder = new AuthBodyBuilder(adminClientId, adminClientSecret);
    }

    @Override
    public boolean verifyUserPassword(String username, String password, String traceId) {
        String url = String.format(VERIFY_USER_CREDENTIALS_URL, realm);
        String token = getTokenManager().getAccessToken().getToken();

        return keyCloakRequest(() ->
                        keycloakRestClient.postJsonForVerification(
                                url,
                                Map.of("password", password, "username", username),
                                token,
                                traceId),
                "verify user password", User.class);
    }

    @Override
    public Optional<AccessTokenResponse> getToken(AuthBodyType authBody) {
        String url = String.format(TOKEN_URL, realm);
        MultiValueMap<String, String> body = authBodyBuilder.build(authBody, getAdminToken());

        return keyCloakRequest(() -> {
            Optional<AccessTokenResponse> response = keycloakRestClient.postFormUrlEncoded(url, body, AccessTokenResponse.class);
            if (response.isPresent()) {
                log.info("Successfully login");
            }
            return response;
        }, "Get user access token", User.class);
    }

    @Override
    public void revokeToken(AuthBodyType authBody) {
        String url = String.format(LOGOUT_URL, realm);
        MultiValueMap<String, String> body = authBodyBuilder.build(authBody, null);

        keyCloakRequest(() -> {
            keycloakRestClient.postFormUrlEncoded(url, body, String.class);
            log.info("Successfully logout");
            return null;
        }, "Revoke user token", User.class);
    }

    @Override
    public RealmResource getRealm() {
        return keycloak.realm(realm);
    }

    @Override
    public UsersResource getUsersResources() {
        return getRealm().users();
    }

    @Override
    public ClientsResource getClientsResource() {
        return getRealm().clients();
    }

    @Override
    public Optional<ClientRepresentation> getClientRep() {
        return getClientsResource().findByClientId(adminClientId).stream().findFirst();
    }

    @Override
    public ClientResource getClientResource() {

        Optional<ClientRepresentation> first = getClientRep().stream().findFirst().stream().findFirst();
        return first.map(clientRepresentation -> getClientsResource().get(clientRepresentation.getId()))
                .orElseThrow();
    }

    @Override
    public RolesResource getClientRolesResource() {
        return getClientResource().roles();
    }

    @Override
    public GroupsResource getGroupsResource() {
        return getRealm().groups();
    }

    @Override
    public List<String> getClientRoles(Map<String, List<String>> clientsRoles) {
        return clientsRoles.getOrDefault(adminClientId, List.of());
    }

    @Override
    public TokenManager getTokenManager() {
        return keycloak.tokenManager();
    }

    private String getAdminToken() {
        return getTokenManager().getAccessToken().getToken();
    }
}
