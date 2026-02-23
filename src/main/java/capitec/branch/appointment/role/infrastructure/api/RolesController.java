package capitec.branch.appointment.role.infrastructure.api;

import capitec.branch.appointment.role.app.*;
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
@RequestMapping("api/v1/roles")
@RequiredArgsConstructor
@Validated
@Log4j2
public class RolesController {

    private final CreateRoleUseCase createRoleUseCase;
    private final DeleteRoleUseCase deleteRoleUseCase;
    private final GetRoleUseCase getRoleUseCase;
    private final CreateRoleTypeUseCase createRoleTypeUseCase;
    private final GetRoleTypeUseCase getRoleTypeUseCase;
    private final AssignRoleToGroupUseCase assignRoleToGroupUseCase;
    private final UserRoleUseCase userRoleUseCase;

    @PostMapping("/client/create")
    @PreAuthorize("hasAnyRole('app_admin')")
    public ResponseEntity<String> createRole(@Valid @RequestBody RoleDTO roleDTO, @RequestHeader("Trace-Id") String traceId) {
        log.info("Creating role. name: {}, traceId: {}", roleDTO.name(), traceId);
        createRoleUseCase.execute(roleDTO, traceId);
        return new ResponseEntity<>("Role created successfully", HttpStatus.CREATED);
    }

    @GetMapping("/client")
    @PreAuthorize("hasAnyRole('app_admin')")
    public ResponseEntity<Role> getRole(@RequestParam("roleName") String roleName, @RequestHeader("Trace-Id") String traceId) {
        log.info("Fetching role. roleName: {}, traceId: {}", roleName, traceId);
        Role clientRole = getRoleUseCase.execute(roleName, traceId);
        return new ResponseEntity<>(clientRole, HttpStatus.OK);
    }

    @DeleteMapping("/client/delete/{roleId}")
    @PreAuthorize("hasAnyRole('app_admin')")
    public ResponseEntity<String> deleteRole(@PathVariable("roleId") String roleId, @RequestHeader("Trace-Id") String traceId) {
        log.info("Deleting role. roleId: {}, traceId: {}", roleId, traceId);
        deleteRoleUseCase.execute(roleId, traceId);
        return new ResponseEntity<>("Role deleted successfully", HttpStatus.ACCEPTED);
    }

    @PutMapping("/client/assign/role/{roleId}/user/{username}")
    @PreAuthorize("'app_admin' and !@securityUtils.isusernameMatching(authentication, #username)")
    public ResponseEntity<String> assignClientRoleToUser(
            @PathVariable("roleId") String roleId,
            @PathVariable("username") String username,
            @RequestHeader("Trace-Id") String traceId) {
        log.info("Assigning client role to user. roleId: {}, username: {}, traceId: {}", roleId, username, traceId);
        userRoleUseCase.assignRoleToUser(username, roleId, traceId);
        return new ResponseEntity<>("Client role assigned to user successfully", HttpStatus.ACCEPTED);
    }

    @GetMapping("/client/user/{username}")
    @PreAuthorize("hasAnyRole('app_admin')")
    public ResponseEntity<Collection<String>> getUserClientRoles(
            @PathVariable("username") String username,
            @RequestHeader("Trace-Id") String traceId) {
        log.info("Fetching user client roles. username: {}, traceId: {}", username, traceId);
        Collection<String> clientRolesForUser = userRoleUseCase.getClientRolesForUser(username, traceId);
        return new ResponseEntity<>(clientRolesForUser, HttpStatus.OK);
    }

    @PostMapping("/group/create")
    @PreAuthorize("hasAnyRole('app_admin')")
    public ResponseEntity<String> createRoleType(
            @Valid @RequestBody RoleTypeDTO roleTypeDTO,
            @RequestHeader("Trace-Id") String traceId) {
        log.info("Creating role type. name: {}, traceId: {}", roleTypeDTO.name(), traceId);
        createRoleTypeUseCase.execute(roleTypeDTO.name(), traceId);
        return new ResponseEntity<>("Role type created successfully", HttpStatus.CREATED);
    }

    @GetMapping("/group")
    @PreAuthorize("hasAnyRole('app_admin')")
    public ResponseEntity<RoleType> getRoleType(
            @RequestParam("name") String name,
            @RequestParam(value = "withRole", required = false, defaultValue = "false") boolean withRole,
            @RequestHeader("Trace-Id") String traceId) {
        log.info("Fetching role type. name: {}, withRole: {}, traceId: {}", name, withRole, traceId);
        RoleType roleType = getRoleTypeUseCase.execute(name, withRole, traceId);
        return new ResponseEntity<>(roleType, HttpStatus.OK);
    }

    @PutMapping("/group/{groupId}/assign/roles")
    @PreAuthorize("hasAnyRole('app_admin')")
    public ResponseEntity<String> assignRoleToGroup(
            @PathVariable("groupId") String groupId,
            @RequestBody Set<String> roleIds,
            @RequestHeader("Trace-Id") String traceId) {
        log.info("Assigning roles to group. groupId: {}, roleIds: {}, traceId: {}", groupId, roleIds, traceId);
        roleIds.forEach(roleId -> assignRoleToGroupUseCase.execute(groupId, roleId, traceId));
        return new ResponseEntity<>("Roles assigned to group successfully", HttpStatus.ACCEPTED);
    }
}
