package capitec.branch.appointment.user.infrastructure.keycloak;

import capitec.branch.appointment.keycloak.domain.KeycloakService;
import capitec.branch.appointment.role.domain.RolesAndGroupsService;
import capitec.branch.appointment.user.app.port.UserRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static capitec.branch.appointment.utils.KeycloakUtils.keyCloakRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRoleServiceImpl implements UserRoleService {

    private final KeycloakService keycloakService;
    private final KeycloakUserHelper userHelper;
    private final RolesAndGroupsService rolesAndGroupsService;

    @Override
    public boolean assignRoleToUser(String username, String roleId) {
        UserResource userResource = userHelper.getUserResource(username);

        var role = rolesAndGroupsService.getClientRoleById(roleId)
                .orElseThrow(() -> {
                    log.warn("Role not found. roleId: {}", roleId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found");
                });

        return keyCloakRequest(() -> {
            ClientRepresentation clientRep = keycloakService.getClientRep()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Client not found"));

            RoleScopeResource roleScopeResource = userResource.roles().clientLevel(clientRep.getId());
            RoleRepresentation roleRepresentation = new RoleRepresentation(role.getName(), role.getDescription(), role.isClientRole());
            roleRepresentation.setId(roleId);
            roleScopeResource.add(List.of(roleRepresentation));

            log.info("Role assigned to user. username: {}, roleId: {}", username, roleId);
            return true;
        }, "assign role to user", Boolean.class);
    }

    @Override
    public Collection<String> getUserRoles(String username) {
        UserResource userResource = userHelper.getUserResource(username);

        return keyCloakRequest(() -> {
            ClientRepresentation clientRep = keycloakService.getClientRep()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Client not found"));

            return userResource.roles().clientLevel(clientRep.getId())
                    .listEffective()
                    .stream()
                    .map(RoleRepresentation::getId)
                    .collect(Collectors.toSet());
        }, "get user roles", Collection.class);
    }

    @Override
    public boolean addUserToGroup(String username, String groupId) {
        UserResource userResource = userHelper.getUserResource(username);

        if (rolesAndGroupsService.getGroupById(groupId).isEmpty()) {
            log.warn("Group not found. groupId: {}", groupId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found");
        }

        return keyCloakRequest(() -> {
            userResource.joinGroup(groupId);
            log.info("User added to group. username: {}, groupId: {}", username, groupId);
            return true;
        }, "add user to group", Boolean.class);
    }

    @Override
    public Collection<String> getUserGroups(String username, int first, int last, boolean briefly) {
        UserResource userResource = userHelper.getUserResource(username);

        return keyCloakRequest(() ->
                        userResource.groups(first, last, briefly)
                                .stream()
                                .map(GroupRepresentation::getId)
                                .collect(Collectors.toSet()),
                "get user groups", Collection.class);
    }
}
