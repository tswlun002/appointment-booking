package capitec.branch.appointment.role.app;

import capitec.branch.appointment.exeption.EntityAlreadyExistException;
import capitec.branch.appointment.role.domain.Role;
import capitec.branch.appointment.role.domain.RoleDTO;
import capitec.branch.appointment.role.domain.RoleService;
import capitec.branch.appointment.role.domain.RoleType;
import capitec.branch.appointment.role.domain.*;
import capitec.branch.appointment.utils.GroupName;
import capitec.branch.appointment.utils.UseCase;
import jakarta.validation.Valid;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

@UseCase
@Validated
@Slf4j
@RequiredArgsConstructor
public class RoleManagementUseCase {

    private final RoleService roleService;

    public  void createClientRole(@Valid RoleDTO role, String traceId) {

        log.info("Create client role:{}, traceId:{}", role, traceId);

       roleService.getClientRole(role.name()).ifPresent(r -> {
            log.error("Role {} already exists", r.getName());
            throw new EntityAlreadyExistException("Role already exists");
        });

        roleService.createRole(new Role(role.name(), role.description(), true));
    }

    public  boolean deleteClientRole(String roleId, String traceId) {

        log.info("Delete client role:{}, traceId:{}", roleId, traceId);

        roleService.getClientRoleById(roleId).orElseThrow(()->{
            log.error("Role {} not found", roleId);
            return new NotFoundException("Role is not found");
        });

        return roleService.deleteClientRole(roleId);
    }

    public Role getClientRole(String roleName, String traceId) {

        return roleService.getClientRole(roleName).
                orElseThrow(()->{
                    log.error("Role:{} not found, traceId:{}",roleName,traceId);
                    return new NotFoundException("Role not found");
                });
    }

    public  void createRoleType(@GroupName String name,String traceId) {

        roleService.getGroup(name,true).ifPresent(g->{
            log.info("Create role type:{}, traceId:{}", name, traceId);
            throw new EntityAlreadyExistException("Role type already exists");
        });

        boolean roleType = roleService.createRoleType(new RoleType(name));

        if(roleType){

            log.info("Role:{} type created, traceId:{}",name, traceId);
        }
        else{

            log.error("Failed to created role:{} type, traceId:{}",name,traceId);
            throw  new ResponseStatusException(HttpStatus.EXPECTATION_FAILED, "Failed to create role type");
        }
    }

    public  void assignRoleToToGroup( String groupId,String roleId, String traceId) {

        log.info("Assign role:{} to group:{}, traceId:{}", roleId,groupId, traceId);

        if(!roleService.checkGroupExistence(groupId)){
            log.error("Group {} is not found", groupId);
            throw new NotFoundException("Group/role-type is not found");

        }

        roleService.getClientRoleById(roleId).orElseThrow(()->{
            log.error("Role {} not found", roleId);
            return new NotFoundException("Role is not found");
        });

        roleService.assignRoleToGroup(groupId,roleId);
    }

    public RoleType getRoleType(String name,boolean withRoles,String traceId) {

       return roleService.getGroup(name, withRoles).orElseThrow(
                ()->
                {
                    log.error("Role  type not found, name:{}, traceId:{}", name, traceId);
                    return new NotFoundException("Role type/group not found");
                }
        );
    }
}
