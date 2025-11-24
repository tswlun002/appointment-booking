package lunga.appointmentbooking.role.domain;

import java.util.Optional;

public interface FetchRoleByNameService {
    Optional<String> getGroupId(String name, boolean briefInfo);
    Optional<String> getClientRoleId(String roleName);


}
