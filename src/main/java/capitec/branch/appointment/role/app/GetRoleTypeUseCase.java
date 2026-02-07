package capitec.branch.appointment.role.app;

import capitec.branch.appointment.role.domain.RoleDomainException;
import capitec.branch.appointment.role.domain.RoleType;
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
public class GetRoleTypeUseCase {

    private final RoleService roleService;

    public RoleType execute(String name, boolean withRoles, String traceId) {
        try {
            log.info("Getting role type. name: {}, withRoles: {}, traceId: {}", name, withRoles, traceId);

            RoleType roleType = roleService.getGroup(name, withRoles)
                    .orElseThrow(() -> {
                        log.warn("Role type not found. name: {}, traceId: {}", name, traceId);
                        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Role type not found");
                    });

            log.debug("Role type retrieved. name: {}, traceId: {}", name, traceId);
            return roleType;
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Validation failed. name: {}, traceId: {}, error: {}", name, traceId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (RoleDomainException e) {
            log.error("Role domain error. name: {}, traceId: {}, error: {}", name, traceId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }
    }
}
