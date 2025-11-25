package capitec.branch.appointment.role.domain;

import capitec.branch.appointment.utils.RoleName;
import lombok.Getter;

import java.util.Objects;
import java.util.UUID;

@Getter
public class Role{
        public static final int MIN_ROLE_NAME_LENGTH = 4;
        @org.hibernate.validator.constraints.UUID
        private  String id ;
        @RoleName
       private final String  name;
       private final String description;
       private final boolean isClientRole;

        public  Role(String name, String description, boolean isClientRole) {
             this.id= UUID.randomUUID().toString();
             this.name = name;
             this.description = description;
             this.isClientRole = isClientRole;
        }

        public  String id() { return id;}

        public  void setId(@org.hibernate.validator.constraints.UUID String id) {
                this.id = id;
        }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Role role)) return false;
        return Objects.equals(id, role.id) ||
                Objects.equals(name, role.name) ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, isClientRole);
    }
}
