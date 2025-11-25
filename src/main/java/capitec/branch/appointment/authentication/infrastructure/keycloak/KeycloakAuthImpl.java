package capitec.branch.appointment.authentication.infrastructure.keycloak;

import capitec.branch.appointment.authentication.domain.AuthService;
import capitec.branch.appointment.authentication.domain.TokenResponse;
import capitec.branch.appointment.keycloak.domain.AuthBodyType;
import capitec.branch.appointment.keycloak.domain.KeycloakService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import capitec.branch.appointment.keycloak.domain.*;

import org.keycloak.representations.AccessTokenResponse;
import org.springframework.stereotype.Component;

import java.util.Optional;


@Slf4j
@RequiredArgsConstructor
@Component
public class KeycloakAuthImpl implements AuthService {

    private final KeycloakService keycloakService;
    private  final  MapTokenResponse mapTokenResponse;

    @Override
    public Optional<TokenResponse> login(String username, String password, String traceId) {

        AuthBodyType.LoginAuthBodyType authBody = new AuthBodyType.LoginAuthBodyType(username, password);
        Optional<AccessTokenResponse> token = keycloakService.getToken(authBody);
        token.ifPresent(tokenResponse -> {
            log.debug("User signing in, traceId:{}, token: {}", traceId,  tokenResponse.getToken());
        });
        return token.map(mapTokenResponse::map);

    }

    @Override
    public boolean verifyCurrentPassword(String username, String password, String traceId) {


        return keycloakService.verifyUserPassword( username,password);
    }

    @Override
    public Optional<TokenResponse> refreshAccessToken(String refreshToken, String traceId) {

        AuthBodyType.RefreshAuthBodyType authBody = new AuthBodyType.RefreshAuthBodyType(refreshToken);
        return keycloakService.getToken(authBody).map(mapTokenResponse::map);
    }

    @Override
    public void logout(String refreshToken, String traceId) {

        AuthBodyType.LogoutAuthBodyType authBody = new AuthBodyType.LogoutAuthBodyType(refreshToken);
        keycloakService.revokeToken(authBody);
    }

    @Override
    public Optional<TokenResponse> impersonateUser(String username) {


        AuthBodyType.ImpersonateAuthBodyType impersonateAuthBodyType = new AuthBodyType.ImpersonateAuthBodyType(username);
        return keycloakService.getToken(impersonateAuthBodyType).map(mapTokenResponse::map) ;
    }

}
