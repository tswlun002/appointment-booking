package capitec.branch.appointment.role.app;

import capitec.branch.appointment.exeption.EntityAlreadyExistException;
import capitec.branch.appointment.role.domain.Role;
import capitec.branch.appointment.role.domain.RoleDomainException;
import capitec.branch.appointment.role.domain.RoleDTO;
import capitec.branch.appointment.role.domain.RoleService;
import capitec.branch.appointment.utils.UseCase;
import capitec.branch.appointment.utils.UseCaseExecutor;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

@UseCase
@Validated
@Slf4j
@RequiredArgsConstructor
public class CreateRoleUseCase {

    private final RoleService roleService;

    public void execute(@Valid RoleDTO role, String traceId) {
        log.info("Creating client role. name: {}, traceId: {}", role.name(), traceId);

        UseCaseExecutor.executeVoid(
                () -> {
                    validateRoleDoesNotExist(role.name(), traceId);
                    createRole(role, traceId);
                    log.info("Client role created successfully. name: {}, traceId: {}", role.name(), traceId);
                },
                "Role creation",
                RoleDomainException.class,
                String.format("name: %s, traceId: %s", role.name(), traceId)
        );
    }

    private void validateRoleDoesNotExist(String roleName, String traceId) {
        roleService.getClientRole(roleName).ifPresent(r -> {
            log.warn("Role already exists. name: {}, traceId: {}", r.getName(), traceId);
            throw new EntityAlreadyExistException("Role already exists");
        });
    }

    private void createRole(RoleDTO role, String traceId) {
        boolean created = roleService.createRole(new Role(role.name(), role.description(), true));
        if (!created) {
            log.error("Failed to create role. name: {}, traceId: {}", role.name(), traceId);
            throw new ResponseStatusException(HttpStatus.EXPECTATION_FAILED, "Failed to create role");
        }
    }
}
