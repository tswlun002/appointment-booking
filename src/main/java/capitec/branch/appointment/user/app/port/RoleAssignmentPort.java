package capitec.branch.appointment.user.app.port;

import java.util.Optional;

/**
 * Port for role/group assignment operations.
 * Implemented by Role context infrastructure.
 */
public interface RoleAssignmentPort {

    /**
     * Gets the group ID by name.
     *
     * @param groupName         the name of the group
     * @param createIfNotExists whether to create the group if it doesn't exist
     * @return the group ID if found
     */
    Optional<String> getGroupId(String groupName, boolean createIfNotExists);
}
