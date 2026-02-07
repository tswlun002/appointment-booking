package capitec.branch.appointment.role.app;

import capitec.branch.appointment.role.domain.Role;
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
public class GetRoleUseCase {

    private final RoleService roleService;

    public Role execute(String roleName, String traceId) {
        try {
            log.info("Getting client role. roleName: {}, traceId: {}", roleName, traceId);

            Role role = roleService.getClientRole(roleName)
                    .orElseThrow(() -> {
                        log.warn("Role not found. roleName: {}, traceId: {}", roleName, traceId);
                        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found");
                    });

            log.debug("Client role retrieved. roleName: {}, traceId: {}", roleName, traceId);
            return role;
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Validation failed. roleName: {}, traceId: {}, error: {}", roleName, traceId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (RoleDomainException e) {
            log.error("Role domain error. roleName: {}, traceId: {}, error: {}", roleName, traceId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }
    }
}
