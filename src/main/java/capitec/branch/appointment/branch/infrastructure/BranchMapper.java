package capitec.branch.appointment.branch.infrastructure;

import capitec.branch.appointment.branch.domain.Branch;
import capitec.branch.appointment.branch.domain.StaffRef;
import capitec.branch.appointment.branch.domain.address.Address;
import capitec.branch.appointment.branch.domain.appointmentinfo.BranchAppointmentInfo;
import capitec.branch.appointment.day.domain.DayType;
import org.mapstruct.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface BranchMapper {

    /**
     * Converts BranchEntity Record to Branch (Domain Model).
     */
    @Mapping(target = "branchId", source = "branchId")
    @Mapping(target = "openTime", source = "openTime")
    @Mapping(target = "closingTime", source = "closingTime")
    @Mapping(target = "address", source = "address")
    @Mapping(target = "branchAppointmentInfo", source = "branchAppointmentInfo")
    @Mapping(target = "weeklyStaff", source = "dailyStaff")
    Branch toDomain(BranchEntity entity);

    /**
     * Converts Branch (Domain Model) to BranchEntity Record.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "updatedAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "branchAppointmentInfo", source = "branchAppointmentInfo")
    @Mapping(target = "dailyStaff", source = "weeklyStaff")
    BranchEntity toEntity(Branch domain);

    // Custom mapping: List<BranchAppointmentInfo> -> Map<DayType, BranchAppointmentInfoEntity>
    default Map<DayType, BranchAppointmentInfoEntity> mapAppointmentInfoToMap(List<BranchAppointmentInfo> infos) {
        if (infos == null || infos.isEmpty()) {
            return null;
        }
        return infos.stream()
                .collect(Collectors.toMap(
                        BranchAppointmentInfo::dayType,
                        info -> new BranchAppointmentInfoEntity(
                                info.slotDuration(),
                                info.utilizationFactor(),
                                info.staffCount(),
                                info.dayType().name()
                        )
                ));
    }

    // Custom mapping: Map<DayType, BranchAppointmentInfoEntity> -> List<BranchAppointmentInfo>
    default List<BranchAppointmentInfo> mapAppointmentInfoToList(Map<DayType, BranchAppointmentInfoEntity> map) {
        if (map == null || map.isEmpty()) {
            return Collections.emptyList();
        }
        return map.entrySet().stream()
                .map(entry -> new BranchAppointmentInfo(
                        entry.getValue().slotDuration(),
                        entry.getValue().utilizationFactor(),
                        entry.getValue().staffCount(),
                        entry.getKey()
                ))
                .toList();
    }

    // Custom mapping: Map<LocalDate, Set<StaffRef>> -> Set<BranchStaffAssignmentEntity>
    default Set<BranchStaffAssignmentEntity> mapWeeklyStaffToSet(Map<LocalDate, Set<StaffRef>> weeklyStaff) {
        if (weeklyStaff == null || weeklyStaff.isEmpty()) {
            return null;
        }
        return weeklyStaff.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream()
                        .map(staffRef -> new BranchStaffAssignmentEntity(
                                staffRef.username(),
                                entry.getKey(),
                                staffRef.status()
                        ))
                )
                .collect(Collectors.toSet());
    }

    // Custom mapping: Set<BranchStaffAssignmentEntity> -> Map<LocalDate, Set<StaffRef>>
    default Map<LocalDate, Set<StaffRef>> mapDailyStaffToMap(Set<BranchStaffAssignmentEntity> dailyStaff) {
        if (dailyStaff == null || dailyStaff.isEmpty()) {
            return Collections.emptyMap();
        }
        return dailyStaff.stream()
                .collect(Collectors.groupingBy(
                        BranchStaffAssignmentEntity::day,
                        Collectors.mapping(
                                assignment -> new StaffRef(
                                        assignment.username(),
                                        assignment.status()
                                ),
                                Collectors.toSet()
                        )
                ));
    }

    // Address mappings
    @Mapping(target = "streetNumber", source = "streetNumber")
    @Mapping(target = "streetName", source = "streetName")
    @Mapping(target = "suburb", source = "suburb")
    @Mapping(target = "city", source = "city")
    @Mapping(target = "province", source = "province")
    @Mapping(target = "postalCode", source = "postalCode")
    @Mapping(target = "country", source = "country")
    Address mapAddress(AddressEntity entity);

    AddressEntity mapAddressToEntity(Address domain);
}
