package capitec.branch.appointment.role.app;

import capitec.branch.appointment.exeption.EntityAlreadyExistException;
import capitec.branch.appointment.role.domain.Role;
import capitec.branch.appointment.role.domain.RoleDomainException;
import capitec.branch.appointment.role.domain.RoleDTO;
import capitec.branch.appointment.role.domain.RoleService;
import capitec.branch.appointment.utils.UseCase;
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
        try {
            log.info("Creating client role. name: {}, traceId: {}", role.name(), traceId);

            validateRoleDoesNotExist(role.name(), traceId);
            createRole(role, traceId);

            log.info("Client role created successfully. name: {}, traceId: {}", role.name(), traceId);
        } catch (EntityAlreadyExistException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(), e);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Validation failed. name: {}, traceId: {}, error: {}", role.name(), traceId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (RoleDomainException e) {
            log.error("Role domain error. name: {}, traceId: {}, error: {}", role.name(), traceId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }
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
