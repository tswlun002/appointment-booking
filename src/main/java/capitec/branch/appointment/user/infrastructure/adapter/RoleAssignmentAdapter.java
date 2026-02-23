package capitec.branch.appointment.user.infrastructure.adapter;

import capitec.branch.appointment.role.domain.FetchRoleByNameService;
import capitec.branch.appointment.user.app.port.RoleAssignmentPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RoleAssignmentAdapter implements RoleAssignmentPort {

    private final FetchRoleByNameService fetchRoleByNameService;

    @Override
    public Optional<String> getGroupId(String groupName, boolean createIfNotExists) {
        return fetchRoleByNameService.getGroupId(groupName, createIfNotExists);
    }
}
