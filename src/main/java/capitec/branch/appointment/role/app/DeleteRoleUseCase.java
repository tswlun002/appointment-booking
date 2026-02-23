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
public class DeleteRoleUseCase {

    private final RoleService roleService;

    public boolean execute(String roleId, String traceId) {
        log.info("Deleting client role. roleId: {}, traceId: {}", roleId, traceId);

        return UseCaseExecutor.execute(
                () -> {
                    validateRoleExists(roleId, traceId);
                    boolean deleted = roleService.deleteClientRole(roleId);

                    if (deleted) {
                        log.info("Client role deleted successfully. roleId: {}, traceId: {}", roleId, traceId);
                    } else {
                        log.error("Failed to delete role. roleId: {}, traceId: {}", roleId, traceId);
                        throw new ResponseStatusException(HttpStatus.EXPECTATION_FAILED, "Failed to delete role");
                    }

                    return deleted;
                },
                "Role deletion",
                RoleDomainException.class,
                String.format("roleId: %s, traceId: %s", roleId, traceId)
        );
    }

    private void validateRoleExists(String roleId, String traceId) {
        roleService.getClientRoleById(roleId).orElseThrow(() -> {
            log.warn("Role not found. roleId: {}, traceId: {}", roleId, traceId);
            return new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found");
        });
    }
}
