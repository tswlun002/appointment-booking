package capitec.branch.appointment.role.app;

import capitec.branch.appointment.role.domain.RoleDomainException;
import capitec.branch.appointment.role.domain.RoleType;
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
public class GetRoleTypeUseCase {

    private final RoleService roleService;

    public RoleType execute(String name, boolean withRoles, String traceId) {
        log.info("Getting role type. name: {}, withRoles: {}, traceId: {}", name, withRoles, traceId);

        return UseCaseExecutor.execute(
                () -> {
                    RoleType roleType = roleService.getGroup(name, withRoles)
                            .orElseThrow(() -> {
                                log.warn("Role type not found. name: {}, traceId: {}", name, traceId);
                                return new ResponseStatusException(HttpStatus.NOT_FOUND, "Role type not found");
                            });

                    log.debug("Role type retrieved. name: {}, traceId: {}", name, traceId);
                    return roleType;
                },
                "Role type retrieval",
                RoleDomainException.class,
                String.format("name: %s, traceId: %s", name, traceId)
        );
    }
}
