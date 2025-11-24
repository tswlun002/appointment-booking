package lunga.appointmentbooking.role.domain;

import jakarta.validation.Valid;

import java.util.Optional;

public interface RoleService extends RolesAndGroupsService, FetchRoleByNameService {

    boolean createRole(@Valid Role role);

    boolean createRoleType(@Valid RoleType roleType);

    boolean deleteClientRole(String roleId);

    boolean assignRoleToGroup(String groupId, String roleId);

    Optional<RoleType> getGroup(String name, boolean briefInfo);

    Optional<Role> getClientRole(String roleName);

    @Override
    default Optional<String> getGroupId(String name, boolean briefInfo) {
        return getGroup(name, briefInfo).map(RoleType::getId);
    }

    @Override
    default Optional<String> getClientRoleId(String roleName) {
        return getClientRole(roleName).map(Role::getId);
    }
}
