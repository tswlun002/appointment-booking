package capitec.branch.appointment.role.app;

import capitec.branch.appointment.exeption.EntityAlreadyExistException;
import capitec.branch.appointment.role.domain.RoleDomainException;
import capitec.branch.appointment.role.domain.RoleService;
import capitec.branch.appointment.role.domain.RoleType;
import capitec.branch.appointment.utils.GroupName;
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
public class CreateRoleTypeUseCase {

    private final RoleService roleService;

    public void execute(@GroupName String name, String traceId) {
        log.info("Creating role type. name: {}, traceId: {}", name, traceId);

        UseCaseExecutor.executeVoid(
                () -> {
                    validateRoleTypeDoesNotExist(name, traceId);
                    createRoleType(name, traceId);
                    log.info("Role type created successfully. name: {}, traceId: {}", name, traceId);
                },
                "Role type creation",
                RoleDomainException.class,
                String.format("name: %s, traceId: %s", name, traceId)
        );
    }

    private void validateRoleTypeDoesNotExist(String name, String traceId) {
        roleService.getGroup(name, true).ifPresent(g -> {
            log.warn("Role type already exists. name: {}, traceId: {}", name, traceId);
            throw new EntityAlreadyExistException("Role type already exists");
        });
    }

    private void createRoleType(String name, String traceId) {
        boolean created = roleService.createRoleType(new RoleType(name));
        if (!created) {
            log.error("Failed to create role type. name: {}, traceId: {}", name, traceId);
            throw new ResponseStatusException(HttpStatus.EXPECTATION_FAILED, "Failed to create role type");
        }
    }
}
