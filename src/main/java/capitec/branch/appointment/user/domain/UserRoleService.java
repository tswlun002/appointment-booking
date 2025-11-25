package capitec.branch.appointment.user.domain;

import java.util.Collection;

public interface UserRoleService {
    boolean assignRoleToUser(String username,String roleId);
    Collection<String> getUserRoles(String username);
    boolean addUserToGroup( String username,String groupId);
    Collection<String> getUserGroups(String username, int first, int last, boolean briefly);
}
