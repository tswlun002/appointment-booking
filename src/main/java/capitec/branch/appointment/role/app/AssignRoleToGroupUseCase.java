package capitec.branch.appointment.role.app;

import capitec.branch.appointment.role.domain.RoleDomainException;
import capitec.branch.appointment.role.domain.RoleService;
import capitec.branch.appointment.utils.UseCase;
import capitec.branch.appointment.utils.UseCaseExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

@UseCase
@Validated
@Slf4j
@RequiredArgsConstructor
public class AssignRoleToGroupUseCase {

    private final RoleService roleService;

    public void execute(String groupId, String roleId, String traceId) {
        log.info("Assigning role to group. groupId: {}, roleId: {}, traceId: {}", groupId, roleId, traceId);

        UseCaseExecutor.executeVoid(
                () -> {
                    validateGroupExists(groupId, traceId);
                    validateRoleExists(roleId, traceId);
                    assignRole(groupId, roleId, traceId);
                    log.info("Role assigned to group successfully. groupId: {}, roleId: {}, traceId: {}", groupId, roleId, traceId);
                },
                "Role assignment",
                RoleDomainException.class,
                String.format("groupId: %s, roleId: %s, traceId: %s", groupId, roleId, traceId)
        );
    }

    private void validateGroupExists(String groupId, String traceId) {
        if (!roleService.checkGroupExistence(groupId)) {
            log.warn("Group not found. groupId: {}, traceId: {}", groupId, traceId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Group/role-type not found");
        }
    }

    private void validateRoleExists(String roleId, String traceId) {
        roleService.getClientRoleById(roleId).orElseThrow(() -> {
            log.warn("Role not found. roleId: {}, traceId: {}", roleId, traceId);
            return new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found");
        });
    }

    private void assignRole(String groupId, String roleId, String traceId) {
        boolean assigned = roleService.assignRoleToGroup(groupId, roleId);
        if (!assigned) {
            log.error("Failed to assign role to group. groupId: {}, roleId: {}, traceId: {}", groupId, roleId, traceId);
            throw new ResponseStatusException(HttpStatus.EXPECTATION_FAILED, "Failed to assign role to group");
        }
    }
}
