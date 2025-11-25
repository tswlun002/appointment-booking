package capitec.branch.appointment.keycloak.domain;


import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.ClientRepresentation;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface KeycloakService {
    Optional<AccessTokenResponse> getToken(AuthBodyType authBody);

    void revokeToken(AuthBodyType authBody);

    boolean verifyUserPassword(String username, String password);

    RealmResource getRealm();

    UsersResource getUsersResources();
    ClientsResource getClientsResource();
    ClientResource getClientResource();

    Optional<ClientRepresentation> getClientRep();

    RolesResource getClientRolesResource();

    GroupsResource getGroupsResource();

    List<String> getClientRoles(Map<String, List<String>> clientsRoles);


}
