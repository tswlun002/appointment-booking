package capitec.branch.appointment.keycloak.domain;

public sealed interface AuthBodyType permits AuthBodyType.ImpersonateAuthBodyType, AuthBodyType.LoginAuthBodyType,
        AuthBodyType.LogoutAuthBodyType, AuthBodyType.RefreshAuthBodyType {

    record LoginAuthBodyType(String username, String password) implements AuthBodyType {}
    record RefreshAuthBodyType(String refreshToken)implements AuthBodyType {}
    record ImpersonateAuthBodyType(String username) implements AuthBodyType { }
    record LogoutAuthBodyType(String refreshToken)implements AuthBodyType {}
}


