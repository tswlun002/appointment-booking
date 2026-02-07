package capitec.branch.appointment.user.app;

import capitec.branch.appointment.user.domain.UserDomainException;
import capitec.branch.appointment.user.domain.UserRoleService;
import capitec.branch.appointment.utils.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;

@UseCase
@Validated
@RequiredArgsConstructor
@Log4j2
public class UserRoleUseCase {

    private final UserRoleService userRoleService;

    public void assignRoleToUser(String username, String roleId, String traceId) {
        try {
            var isAssigned = userRoleService.assignRoleToUser(username, roleId);
            if (!isAssigned) {
                log.error("Failed to assign role {} to user {}, traceId:{}", roleId, username, traceId);
                throw new ResponseStatusException(HttpStatus.EXPECTATION_FAILED, "Failed to assign role " + roleId + " to user");
            }
            log.info("User role {} assigned to user {}, traceId:{}", roleId, username, traceId);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Validation failed. username: {}, traceId: {}, error: {}", username, traceId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (UserDomainException e) {
            log.error("User domain error. username: {}, traceId: {}, error: {}", username, traceId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }
    }

    public Collection<String> getClientRolesForUser(String username, String traceId) {
        try {
            Collection<String> clientRolesForUser = userRoleService.getUserRoles(username);
            if (clientRolesForUser.isEmpty()) {
                log.error("User not found with Username: {}, traceId:{}", username, traceId);
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
            }
            log.info("User:{} client roles {}, traceId:{}", username, clientRolesForUser, traceId);
            return clientRolesForUser;
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Validation failed. username: {}, traceId: {}, error: {}", username, traceId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (UserDomainException e) {
            log.error("User domain error. username: {}, traceId: {}, error: {}", username, traceId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }
    }
}
