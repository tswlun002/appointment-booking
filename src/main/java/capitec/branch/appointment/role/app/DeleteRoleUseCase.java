package capitec.branch.appointment.role.app;

import capitec.branch.appointment.role.domain.RoleDomainException;
import capitec.branch.appointment.role.domain.RoleService;
import capitec.branch.appointment.utils.UseCase;
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
        try {
            log.info("Deleting client role. roleId: {}, traceId: {}", roleId, traceId);

            validateRoleExists(roleId, traceId);
            boolean deleted = roleService.deleteClientRole(roleId);

            if (deleted) {
                log.info("Client role deleted successfully. roleId: {}, traceId: {}", roleId, traceId);
            } else {
                log.error("Failed to delete role. roleId: {}, traceId: {}", roleId, traceId);
                throw new ResponseStatusException(HttpStatus.EXPECTATION_FAILED, "Failed to delete role");
            }

            return deleted;
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Validation failed. roleId: {}, traceId: {}, error: {}", roleId, traceId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (RoleDomainException e) {
            log.error("Role domain error. roleId: {}, traceId: {}, error: {}", roleId, traceId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }
    }

    private void validateRoleExists(String roleId, String traceId) {
        roleService.getClientRoleById(roleId).orElseThrow(() -> {
            log.warn("Role not found. roleId: {}, traceId: {}", roleId, traceId);
            return new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found");
        });
    }
}
