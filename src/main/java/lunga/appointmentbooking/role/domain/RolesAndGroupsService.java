package lunga.appointmentbooking.role.domain;

import java.util.Optional;

public interface RolesAndGroupsService {
    Optional<RoleType> getGroupById(String roleTyeId);
    Optional<Role> getClientRoleById(String roleId);
    default boolean checkGroupExistence(String groupId){
        return getGroupById(groupId).isPresent();
    }

}
