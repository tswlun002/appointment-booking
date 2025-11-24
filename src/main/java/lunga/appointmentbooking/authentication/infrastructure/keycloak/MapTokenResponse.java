package lunga.appointmentbooking.authentication.infrastructure.keycloak;

import lunga.appointmentbooking.authentication.domain.TokenResponse;
import org.keycloak.representations.AccessTokenResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MapTokenResponse {
    @Mapping(source = "token", target = "token")
    @Mapping(source = "expiresIn", target = "expiresIn")
    @Mapping(source = "refreshExpiresIn", target = "refreshExpiresIn")
    @Mapping(source = "refreshToken", target = "refreshToken")
    @Mapping(source = "tokenType", target = "tokenType")
    @Mapping(source = "idToken", target = "idToken")
    @Mapping(source = "notBeforePolicy", target = "notBeforePolicy")
    @Mapping(source = "sessionState", target = "sessionState")
    @Mapping(source = "otherClaims", target = "otherClaims")
    @Mapping(source = "scope", target = "scope")
    @Mapping(source = "error", target = "error")
    @Mapping(source = "errorDescription", target = "errorDescription")
    @Mapping(source = "errorUri", target = "errorUri")
    TokenResponse map(AccessTokenResponse accessTokenResponse);
}
