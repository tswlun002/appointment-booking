package lunga.appointmentbooking.keycloak.domain;

public sealed interface  KeycloakAuthBodyType permits KeycloakLoginAuthBodyType, KeycloakLogoutAuthBodyType,
KeycloakRefreshAuthBodyType, KeycloakImpersonateAuthBodyType{

}





