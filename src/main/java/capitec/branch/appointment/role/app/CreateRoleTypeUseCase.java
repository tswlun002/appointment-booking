package capitec.branch.appointment.role.app;

import capitec.branch.appointment.exeption.EntityAlreadyExistException;
import capitec.branch.appointment.role.domain.RoleDomainException;
import capitec.branch.appointment.role.domain.RoleService;
import capitec.branch.appointment.role.domain.RoleType;
import capitec.branch.appointment.utils.GroupName;
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
public class CreateRoleTypeUseCase {

    private final RoleService roleService;

    public void execute(@GroupName String name, String traceId) {
        try {
            log.info("Creating role type. name: {}, traceId: {}", name, traceId);

            validateRoleTypeDoesNotExist(name, traceId);
            createRoleType(name, traceId);

            log.info("Role type created successfully. name: {}, traceId: {}", name, traceId);
        } catch (EntityAlreadyExistException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(), e);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Validation failed. name: {}, traceId: {}, error: {}", name, traceId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (RoleDomainException e) {
            log.error("Role domain error. name: {}, traceId: {}, error: {}", name, traceId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }
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
