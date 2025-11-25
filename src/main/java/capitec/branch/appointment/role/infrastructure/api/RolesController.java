package capitec.branch.appointment.role.infrastructure.api;

import capitec.branch.appointment.role.app.RoleManagementUseCase;
import capitec.branch.appointment.role.domain.Role;
import capitec.branch.appointment.role.domain.RoleDTO;
import capitec.branch.appointment.role.domain.RoleType;
import capitec.branch.appointment.user.app.UserRoleUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Set;

@RestController
@RequestMapping("users-service/roles")
@RequiredArgsConstructor
@Validated
@Log4j2
public class RolesController {
    private final RoleManagementUseCase roleManagementUseCase;
    private final UserRoleUseCase userRoleUseCase;

    @PostMapping("/client/create")
    public ResponseEntity<?> createRole(@Valid @RequestBody RoleDTO roleDTO, @RequestHeader("Trace-Id") String traceId) {
        log.info("Role {}, traceId:{}", roleDTO, traceId);
        roleManagementUseCase.createClientRole(roleDTO, traceId);
        return new ResponseEntity<>("Role created successfully",HttpStatus.CREATED);
    }
    @GetMapping("/client")
    public ResponseEntity<?> getRole( @RequestParam("roleName") String roleName, @RequestHeader("Trace-Id") String traceId) {
        log.info("Fetch role {}, traceId:{}", roleName, traceId);
        Role clientRole = roleManagementUseCase.getClientRole(roleName, traceId);
        return new ResponseEntity<>(clientRole,HttpStatus.OK);
    }
    @DeleteMapping("/client/delete/{roleId}")
    public ResponseEntity<?> deleteRole( @PathVariable("roleId") String roleId, @RequestHeader("Trace-Id") String traceId) {
        log.info("Delete role {}, traceId:{}", roleId, traceId);
        roleManagementUseCase.deleteClientRole(roleId, traceId);
        return new ResponseEntity<>("Deleted role successfully",HttpStatus.ACCEPTED);
    }

    @PutMapping("/client/assign/role/{roleId}/user/{username}")
    @PreAuthorize("!@securityUtils.isusernameMatching(authentication, #username)")  //user cannot assign themselves role
    public ResponseEntity<?> assignClientRoleToUser(@PathVariable("roleId") String roleId,
                                                    @PathVariable("username") String username,
                                                    @RequestHeader("Trace-Id") String traceId) {
        log.info("Assign client role {}, to user:{} traceId:{}", roleId, username, traceId);
        userRoleUseCase.assignRoleToUser( username,roleId, traceId);
        return new ResponseEntity<>("Assign client role to user successfully",HttpStatus.ACCEPTED);
    }

    @GetMapping("/client/user/{username}")
    public ResponseEntity<?> getUserClientRoles(@PathVariable("username") String username, @RequestHeader("Trace-Id") String traceId) {
        log.info("Fetch user:{} client roles, traceId:{}",username, traceId);
        Collection<String> clientRolesForUser = userRoleUseCase.getClientRolesForUser(username, traceId);
        return new ResponseEntity<>(clientRolesForUser,HttpStatus.OK);
    }

    @PostMapping("/group/create")
    public ResponseEntity<?> createRoleType(@Valid @RequestBody RoleTypeDTO roleTypeDTO, @RequestHeader("Trace-Id") String traceId) {
        log.info("Create role:{} type {}", roleTypeDTO,roleTypeDTO);
        roleManagementUseCase.createRoleType(roleTypeDTO.name(),traceId);
        return new ResponseEntity<>("Role type/group created successfully",HttpStatus.CREATED);
    }

    @GetMapping("/group")
    public ResponseEntity<?> getRoleType(@RequestParam("name") String name,@RequestParam(value = "withRole",required = false) Boolean withRole ,@RequestHeader("Trace-Id") String traceId) {
        log.info("Fetch role type:{} traceId:{}", name,traceId);
        RoleType roleTypeWithRoles = roleManagementUseCase.getRoleType(name,withRole, traceId);
        return new ResponseEntity<>(roleTypeWithRoles,HttpStatus.OK);

    }

    @PutMapping("/group/{groupId}/assign/roles")
    public  ResponseEntity<?> assignRoleToGroup(@PathVariable("groupId")String groupId, @RequestBody Set<String> roleIds, @RequestHeader("Trace-Id") String traceId) {
        log.info("Assign role to group {}, to role:{} traceId:{}", String.join(", ",groupId), roleIds, traceId);
        roleIds.forEach(roleId-> roleManagementUseCase.assignRoleToToGroup(groupId, roleId,traceId));
        return new ResponseEntity<>("Assign role to group successfully",HttpStatus.ACCEPTED);
    }


}
