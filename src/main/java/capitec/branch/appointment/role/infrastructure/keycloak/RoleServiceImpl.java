package capitec.branch.appointment.role.infrastructure.keycloak;

import jakarta.ws.rs.core.Response;
import capitec.branch.appointment.keycloak.domain.KeycloakService;
import capitec.branch.appointment.role.domain.Role;
import capitec.branch.appointment.role.domain.RoleDomainException;
import capitec.branch.appointment.role.domain.RoleService;
import capitec.branch.appointment.role.domain.RoleType;
import jakarta.validation.Valid;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static capitec.branch.appointment.utils.KeycloakUtils.keyCloakRequest;

@Slf4j
@Component
@RequiredArgsConstructor
@Validated
public class RoleServiceImpl implements RoleService {


    private  final KeycloakService keycloakService;



    public static Function<RoleRepresentation, Role> roleRepresentationToRole() {
        return role -> {
            var role1 = new Role(role.getName(), role.getDescription(), role.getClientRole());
            role1.setId(role.getId());
            return role1;
        };
    }

    public Function<GroupRepresentation, RoleType> groupRepresentationRoleTypeFunction() {
        return groupRepresentation -> {
            Map<String, List<String>> clientRoles = groupRepresentation.getClientRoles();

            Set<String> rolesIds = clientRoles!=null && !clientRoles.isEmpty() ?

                    keycloakService.getClientRoles(clientRoles)
                            .stream()
                            .map(r -> getClientRole(r).orElseThrow().getId())
                            .collect(Collectors.toSet())

                    : Collections.emptySet();

            RoleType roleType = new RoleType(groupRepresentation.getName(), rolesIds);
            roleType.setId(groupRepresentation.getId());

            return roleType;
        };
    }


    @Override
    public boolean createRole(@Valid Role role) {
        RoleRepresentation roleRepresentation = new RoleRepresentation();
        roleRepresentation.setName(role.getName());
        roleRepresentation.setDescription(role.getDescription());
        roleRepresentation.setClientRole(role.isClientRole());

        return keyCloakRequest(() -> {
            keycloakService.getClientRolesResource().create(roleRepresentation);
            log.info("Role created: {}", roleRepresentation);
            var clientRole = getClientRole(role.getName()).orElseThrow();
            role.setId(clientRole.getId());
            return true;
        }, "creat role", Role.class);

    }

    @Override
    public boolean createRoleType(@Valid RoleType roleType) {

        GroupRepresentation groupRepresentation = new GroupRepresentation();
        groupRepresentation.setName(roleType.getName());
        ClientRepresentation representation = keycloakService.getClientRep().orElseThrow();

        return keyCloakRequest(() -> {

            GroupsResource groupsResource = keycloakService.getGroupsResource();
            try (Response response = groupsResource.add(groupRepresentation)) {

                var status = HttpStatus.valueOf(response.getStatus());

                if (status.is2xxSuccessful()) {

                    GroupRepresentation groupRepresentation1 = groupRepresentation(groupsResource,roleType.getName(), false).orElseThrow();

                    groupsResource.group(groupRepresentation1.getId())
                            .roles().clientLevel(representation.getId())
                            .add(
                                    roleType.getRolesIds().stream().map(
                                            roleId -> getClientRoleRepresentationById(keycloakService.getRealm(),roleId).
                                                    orElseThrow(() -> {

                                                        log.error("Role:{} is not found", roleId);
                                                        return new RoleDomainException("Role not found: " + roleId);
                                                    })
                                    ).toList()
                            );
                    roleType.setId(groupRepresentation1.getId());
                    log.info("Role created: {}", roleType);
                    return true;
                }
                log.error("Failed to create role type. status: {}, reason: {}", status, response.getStatusInfo().getReasonPhrase());
                throw new RoleDomainException("Failed to create role type: " + response.getStatusInfo().getReasonPhrase());
            }

        }, " create role type/group", RoleType.class);
    }

    private Optional<GroupRepresentation> groupRepresentation(GroupsResource groupsResource,String name, boolean briefInfo) {

        return keyCloakRequest(() -> groupsResource.groups(name, true, 0, 1, briefInfo).
                stream().
                findFirst(), " get Group resource ", GroupRepresentation.class);
    }

    private Optional<GroupRepresentation> groupRepresentation(GroupsResource groupsResource,String roleTypeId) {

        return keyCloakRequest(() -> {
                    GroupResource group = groupsResource.group(roleTypeId);
                    Optional<GroupRepresentation> groupRepresentation;
                    try {
                        groupRepresentation = Optional.of(group.toRepresentation());
                    } catch (NotFoundException e) {
                        log.warn("Group {} not found", roleTypeId);
                        groupRepresentation = Optional.empty();
                    }
                    return groupRepresentation;
                }
                , " get Group resource ", org.keycloak.representations.idm.GroupRepresentation.class);
    }

    @Override
    public Optional<RoleType> getGroup(String name, boolean briefInfo) {

        return keyCloakRequest(() ->

                        groupRepresentation(keycloakService.getGroupsResource(),name, briefInfo).map(g -> groupRepresentationRoleTypeFunction().apply(g)),
                " get role type/group", RoleType.class);

    }

    @Override
    public Optional<Role> getClientRole(String roleName) {

        return getClientRoleRepresentation(keycloakService.getClientRolesResource(),roleName).map(r ->
                {
                    Role role = roleRepresentationToRole().apply(r);
                    log.info("Role retrieved: {}", role);
                    return role;
                }
        );
    }
    private Optional<RoleRepresentation> getClientRoleRepresentationById(RealmResource realmResource,String roleId) {

        return keyCloakRequest(() ->
                {
                    Optional<RoleRepresentation> representation;
                    try {
                        representation = Optional.of(realmResource.rolesById().getRole(roleId));

                    } catch (NotFoundException e) {
                        representation = Optional.empty();
                    }
                    return representation;
                }
                , "get role", RoleRepresentation.class);
    }
    private Optional<RoleRepresentation> getClientRoleRepresentation( RolesResource rolesResource,String roleName) {

        return keyCloakRequest(() ->
                {
                    var roleResource = getClientRoleResources(rolesResource,roleName);
                    Optional<RoleRepresentation> representation;
                    try {
                        representation = roleResource.map(RoleResource::toRepresentation);

                    } catch (NotFoundException e) {
                        representation = Optional.empty();
                    }
                    return representation;
                }
                , "get role", RoleRepresentation.class);
    }

    private Optional<RoleResource> getClientRoleResources(RolesResource rolesResource,String roleName) {
        return Optional.of(
                keyCloakRequest(() ->
                                rolesResource.get(roleName)
                        , "get role", Role.class)
        );
    }

    @Override
    public boolean deleteClientRole(String roleId) {
        RoleByIdResource roleByIdResource = keycloakService.getRealm().rolesById();

        return keyCloakRequest(() -> {
            roleByIdResource.deleteRole(roleId);
            log.debug("Role removed: {}", roleId);
            return true;
        }, "delete role", Role.class);


    }



    @Override
    public boolean assignRoleToGroup(String groupId, String roleId) {


        var roleRepresentation = getClientRoleRepresentationById(keycloakService.getRealm(),roleId).orElseThrow();

        return keyCloakRequest(() -> {

            ClientRepresentation clientRepresentation = keycloakService.getClientRep().orElseThrow();

            keycloakService.getGroupsResource().group(groupId).
                    roles().
                    clientLevel(clientRepresentation.getId())
                    .add(Collections.singletonList(roleRepresentation));

            log.debug("Role assigned to group: {}", groupId);

            return true;
        }, " assign role to role type/group ", Role.class);
    }


    @Override
    public Optional<RoleType> getGroupById(String roleTyeId) {

        Optional<GroupRepresentation> groupRepresentation = groupRepresentation(keycloakService.getGroupsResource(),roleTyeId);

        return keyCloakRequest(() ->

                        groupRepresentation.map(g -> groupRepresentationRoleTypeFunction().apply(g))

                , " get role type/group by eventId ", RoleType.class);

    }

    @Override
    public Optional<Role> getClientRoleById(String roleId) {


        return keyCloakRequest(() -> {
            Optional<RoleRepresentation> representation;
            try {
                RoleByIdResource roleByIdResource = keycloakService.getRealm().rolesById();
                representation = Optional.of(roleByIdResource.getRole(roleId));
            } catch (NotFoundException e) {
                representation = Optional.empty();
            }
            return representation.map(r -> roleRepresentationToRole().apply(r));
        }, " get role by eventId ", Role.class);
    }
}
