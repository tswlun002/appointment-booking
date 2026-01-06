package capitec.branch.appointment.branch.infrastructure;

import capitec.branch.appointment.branch.domain.Branch;
import capitec.branch.appointment.branch.domain.address.Address;
import capitec.branch.appointment.branch.domain.appointmentinfo.BranchAppointmentInfo;
import capitec.branch.appointment.day.domain.DayType;
import org.mapstruct.*;

import java.time.Duration;
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
    Branch toDomain(BranchEntity entity);

    /**
     * Converts Branch (Domain Model) to BranchEntity Record.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "updatedAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "branchAppointmentInfo", expression = "java(capitec.branch.appointment.branch.infrastructure.BranchMapper.mapAppointmentInfoToMap(domain))")
    //@Mapping(target = "dailyStaff", expression = "java(capitec.branch.appointment.branch.infrastructure.BranchMapper.mapWeeklyStaffToSet(domain))")
    BranchEntity toEntity(Branch domain);

    // Custom mapping: List<BranchAppointmentInfo> -> Map<DayType, BranchAppointmentInfoEntity>
    static   Map<DayType, BranchAppointmentInfoEntity> mapAppointmentInfoToMap(Branch branch) {
        if(branch == null) {
            return null;
        }
        List<BranchAppointmentInfo> infos = branch.getBranchAppointmentInfo();
        if (infos == null || infos.isEmpty()) {
            return null;
        }
        return infos.stream()
                .collect(Collectors.toMap(
                        BranchAppointmentInfo::dayType,
                        info -> new BranchAppointmentInfoEntity(
                                branch.getBranchId(),
                                Math.toIntExact(info.slotDuration().toMinutes()),
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
                        Duration.ofMinutes(entry.getValue().slotDuration()),
                        entry.getValue().utilizationFactor(),
                        entry.getValue().staffCount(),
                        entry.getKey()
                ))
                .toList();
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
