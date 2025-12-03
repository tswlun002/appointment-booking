package capitec.branch.appointment.keycloak.infrastructure;

import capitec.branch.appointment.keycloak.domain.AuthBodyType;
import capitec.branch.appointment.keycloak.domain.KeycloakService;
import lombok.extern.slf4j.Slf4j;
import capitec.branch.appointment.keycloak.domain.*;
import capitec.branch.appointment.user.domain.User;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.*;
import org.keycloak.admin.client.token.TokenManager;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.ClientRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.MultiValueMapAdapter;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

import static capitec.branch.appointment.utils.KeycloakUtils.keyCloakRequest;

@Slf4j
@Service
public class KeycloakServiceImpl implements KeycloakService {

    private final RestClient restClient;
    public static final String VERIFY_USER_CREDENTIALS_URL = "/realms/%s/verify/password";
    public static final String TOKEN_URL = "/realms/%s/protocol/openid-connect/token";
    public static final String LOGOUT_URL = "/realms/%s/protocol/openid-connect/logout";
    private final Keycloak keycloak;
    @Value("${keycloak.realm}")
    private String realm;
    @Value("${keycloak.adminClientId}")
    private String adminClientId;
    @Value("${keycloak.adminClientSecret}")
    private String adminClientSecret;

    public KeycloakServiceImpl(RestClient restClient, Keycloak keycloak) {
        this.restClient = restClient;
        this.keycloak = keycloak;

    }


    @Override
    public boolean verifyUserPassword(String username, String password, String traceId) {

        var url = String.format(VERIFY_USER_CREDENTIALS_URL, realm);

        return keyCloakRequest(() ->
                        restClient.post()
                                .uri(url)
                                .body(Map.of("password", password, "username", username))
                                .headers(h -> {
                                    h.add(HttpHeaders.AUTHORIZATION, "Bearer " + keycloak.tokenManager().getAccessToken().getToken());
                                    h.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                                    h.add("Trace-Id", traceId);
                                })
                                .contentType(MediaType.APPLICATION_JSON)
                                .exchange((clientRequest, clientResponse) -> {
                                    if (clientResponse.getStatusCode().is2xxSuccessful()) {

                                        log.info("Successfully verified user password");
                                        return true;

                                    }
                                    if (clientResponse.getStatusCode().equals(HttpStatus.UNAUTHORIZED)) {

                                        log.info("Unauthorized user password, Exception trace:{}", clientResponse.bodyTo(String.class));
                                        return false;
                                    }
                                    log.error("Failed to verify user password,\nrequest:{},\nresponse:{}", clientRequest, clientResponse);
                                    throw new ResponseStatusException(clientResponse.getStatusCode(), clientResponse.getStatusText());
                                })
                , " verify user password ", User.class);
    }

    @Override
    public Optional<AccessTokenResponse> getToken(AuthBodyType authBody) {

        MultiValueMap<String, String> authDetails = getAuthDetails(authBody);

        var url = String.format(TOKEN_URL, realm);
        return keyCloakRequest(() ->

                        restClient.post()
                                .uri(url)
                                .body(authDetails)
                                .headers(h -> {
                                    h.add(jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);
                                    h.add("Accept-Encoding", "gzip, deflate, br");
                                    h.add("User-Agent", "Mozilla/5.0");
                                    h.add("Accept", MediaType.ALL_VALUE);
                                })
                                .accept(MediaType.ALL)
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .exchange((_, clientResponse) -> {
                                    if (clientResponse.getStatusCode().is2xxSuccessful()) {
                                        log.info("Successfully login");
                                        return Optional.of(Objects.requireNonNull(clientResponse.bodyTo(AccessTokenResponse.class)));
                                    } else {
                                        log.error("Failed to login ");
                                        throw new ResponseStatusException(clientResponse.getStatusCode(), clientResponse.getStatusText());
                                    }
                                })
                , "Get user access token", User.class);
    }
    @Override
    public void revokeToken(AuthBodyType authBody) {

        var authDetails = getAuthDetails(authBody);

        var url = String.format(LOGOUT_URL, realm);
        keyCloakRequest(() ->

                        restClient.post()
                                .uri(url)
                                .body(authDetails)
                                .headers(h -> {
                                    h.add(jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);
                                    h.add("Accept-Encoding", "gzip, deflate, br");
                                    h.add("User-Agent", "Mozilla/5.0");
                                    h.add("Accept", MediaType.ALL_VALUE);
                                })
                                .accept(MediaType.ALL)
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .exchange((_, clientResponse) -> {
                                    if (clientResponse.getStatusCode().is2xxSuccessful()) {
                                        log.info("Successfully logout");
                                        return Optional.of(Objects.requireNonNull(clientResponse.bodyTo(String.class)));
                                    } else {
                                        log.error("Failed to logout");
                                        throw new ResponseStatusException(clientResponse.getStatusCode(), clientResponse.getStatusText());
                                    }
                                })
                , "Revoke user token ", User.class);
    }

    private MultiValueMap<String, String> getAuthDetails(AuthBodyType authBodyType) {
        return switch (authBodyType) {
            case AuthBodyType.LoginAuthBodyType login -> getLoginBody(login);
            case AuthBodyType.RefreshAuthBodyType refresh -> getRefreshBody(refresh);
            case AuthBodyType.LogoutAuthBodyType logout ->getLogoutBody(logout);
            case AuthBodyType.ImpersonateAuthBodyType impersonate -> {
                String token = keycloak.tokenManager().getAccessToken().getToken();
                yield getImpersonationBody(impersonate, token);
            }
        };
    }
    private MultiValueMapAdapter<String, String> getLogoutBody(AuthBodyType.LogoutAuthBodyType logout) {
        LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();

        params.add("client_id", adminClientId);
        params.add("client_secret", adminClientSecret);
        params.add("grant_type", "password");
        params.add("refresh_token", logout.refreshToken());
        return params;

    }
    private MultiValueMapAdapter<String, String> getRefreshBody(AuthBodyType.RefreshAuthBodyType refresh) {
        LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();

        params.add("client_id", adminClientId);
        params.add("client_secret", adminClientSecret);
        params.add("scope", "openid profile email");
        params.add("grant_type", "refresh_token");
        params.add("refresh_token", refresh.refreshToken());
        return params;

    }
    private MultiValueMapAdapter<String, String> getLoginBody(AuthBodyType.LoginAuthBodyType login) {
        LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();

        params.add("client_id", adminClientId);
        params.add("client_secret", adminClientSecret);
        params.add("scope", "openid profile email");
        params.add("grant_type", "password");
        params.add("username", login.username());
        params.add("password", login.password());
        return params;

    }

    private MultiValueMapAdapter<String, String> getImpersonationBody(AuthBodyType.ImpersonateAuthBodyType impersonate, String token) {
        LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange");
        params.add("client_id", adminClientId);
        params.add("client_secret", adminClientSecret);
        params.add("subject_token_type", "urn:ietf:params:oauth:token-type:access_token");
        params.add("scope", "openid profile email");
        params.add("subject_token", token);
        params.add("requested_subject", impersonate.username());
        return params;
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
    public ClientsResource getClientsResource() {

       return getRealm().clients();
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
        return clientsRoles.get(adminClientId);
    }

    @Override
    public TokenManager getTokenManager() {
        return keycloak.tokenManager();
    }


    private MultiValueMap<String, String> getCookies(RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse clientResponse) {

        List<String> obj = clientResponse.getHeaders().get(jakarta.ws.rs.core.HttpHeaders.SET_COOKIE);
        var isThereCookie = (obj != null && !obj.isEmpty());
        return new MultiValueMapAdapter<>(isThereCookie ? Map.of(jakarta.ws.rs.core.HttpHeaders.COOKIE, obj) : Map.of());

    }


}
