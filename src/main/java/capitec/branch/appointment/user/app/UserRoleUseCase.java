package capitec.branch.appointment.user.app;

import capitec.branch.appointment.user.domain.UserRoleService;
import capitec.branch.appointment.utils.UseCase;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;

@UseCase
@Validated
@RequiredArgsConstructor
@Log4j2
public class UserRoleUseCase {

    private  final UserRoleService userRoleService;

    public  void assignRoleToUser( String username,String roleId, String traceId) {
        var isAssigned= userRoleService.assignRoleToUser(username,roleId );
        if(! isAssigned) {
            log.error("Failed to assign role {} to user {}, traceId:{}", roleId, username,traceId);
            throw new IllegalStateException("Failed to assign role " + roleId + " to user ");
        }
        log.info("User role {} assigned to user {}, traceId:{}", roleId, username,traceId);
    }
    public Collection<String> getClientRolesForUser(String username, String traceId) {
        Collection<String> clientRolesForUser = userRoleService.getUserRoles(username);
        if(clientRolesForUser.isEmpty()) {
            log.error("User  not found with Username: {}, traceId:{}",username, traceId);
            throw new NotFoundException("User  not found");
        }
        log.info("User:{}  client roles {}, traceId:{}",username ,clientRolesForUser,traceId);
        return clientRolesForUser;
    }
}
