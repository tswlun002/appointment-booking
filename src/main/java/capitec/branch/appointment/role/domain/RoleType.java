package capitec.branch.appointment.role.domain;

import capitec.branch.appointment.utils.GroupName;
import lombok.Getter;
import org.hibernate.validator.constraints.UUID;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
@Getter
public class RoleType {
    @UUID
    private String id;
    @GroupName
    private final String name;
    private Set<String> rolesIds;

    public RoleType(String name) {
        this.name = name;
        this.rolesIds = new HashSet<>();
        this.id= java.util.UUID.randomUUID().toString();
    }

    public RoleType(String name, Set<String> rolesIds) {
        this.name = name;
        this.rolesIds = rolesIds;
        this.id= java.util.UUID.randomUUID().toString();
    }

    public  void  setId(@UUID String id) {
        this.id = id;
    }
    public void addRole(@UUID String roleId) {
        rolesIds.add(roleId);
    }

    public void setRole(@UUID  Set<String> role) {
        this.rolesIds = role;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RoleType roleType)) return false;
        return Objects.equals(id, roleType.id) && Objects.equals(name, roleType.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }
}
