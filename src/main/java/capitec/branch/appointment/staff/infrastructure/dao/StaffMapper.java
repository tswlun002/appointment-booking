package capitec.branch.appointment.staff.infrastructure.dao;

import capitec.branch.appointment.staff.domain.Staff;
import capitec.branch.appointment.staff.domain.StaffStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
interface StaffMapper {

    /**
     * Converts a StaffEntity to the Staff domain record.
     */
    @Mapping(target = "status", source = "status", qualifiedByName = "mapToStaffStatus")
    Staff toDomain(StaffEntity entity);

    /**
     * Converts a Staff domain record to the StaffEntity data record.
     */
    @Mapping(target = "status", source = "status", qualifiedByName = "mapToString")
    
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "updatedAt", ignore = true) 
    StaffEntity toEntity(Staff domain);

    @Named("mapToStaffStatus")
    default StaffStatus mapToStaffStatus(String status) {
        if (status == null) {
            return null;
        }
        return StaffStatus.valueOf(status.toUpperCase());
    }

    @Named("mapToString")
    default String mapToString(StaffStatus status) {
        if (status == null) {
            return null;
        }
        return status.name();
    }
}