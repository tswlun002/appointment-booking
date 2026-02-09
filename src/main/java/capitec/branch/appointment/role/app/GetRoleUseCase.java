package capitec.branch.appointment.role.app;

import capitec.branch.appointment.role.domain.Role;
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
public class GetRoleUseCase {

    private final RoleService roleService;

    public Role execute(String roleName, String traceId) {
        log.info("Getting client role. roleName: {}, traceId: {}", roleName, traceId);

        return UseCaseExecutor.execute(
                () -> {
                    Role role = roleService.getClientRole(roleName)
                            .orElseThrow(() -> {
                                log.warn("Role not found. roleName: {}, traceId: {}", roleName, traceId);
                                return new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found");
                            });

                    log.debug("Client role retrieved. roleName: {}, traceId: {}", roleName, traceId);
                    return role;
                },
                "Role retrieval",
                RoleDomainException.class,
                String.format("roleName: %s, traceId: %s", roleName, traceId)
        );
    }
}
