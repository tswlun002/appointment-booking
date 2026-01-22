package capitec.branch.appointment.branch.infrastructure;

import capitec.branch.appointment.branch.domain.Branch;
import capitec.branch.appointment.branch.domain.appointmentinfo.BranchAppointmentInfo;
import capitec.branch.appointment.branch.domain.operationhours.OperationHoursOverride;
import org.mapstruct.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
interface BranchMapper {

    /**
     * Converts BranchEntity Record to Branch (Domain Model).
     */
    @Mapping(target = "branchId", source = "branchId")
    @Mapping(target = "operationHoursOverride", source = "operationHoursOverride")
    @Mapping(target = "branchAppointmentInfo", source = "branchAppointmentInfo")
    Branch toDomain(BranchEntity entity);

    /**
     * Converts Branch (Domain Model) to BranchEntity Record.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "updatedAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "branchAppointmentInfo", expression = "java(capitec.branch.appointment.branch.infrastructure.BranchMapper.mapAppointmentInfoToMap(domain))")
    @Mapping(target = "branchAppointmentInfo", expression = "java(capitec.branch.appointment.branch.infrastructure.BranchMapper.mapOperationHoursOverrideToMap(domain))")

    BranchEntity toEntity(Branch domain);

    static   Map<LocalDate, BranchAppointmentInfoEntity> mapAppointmentInfoToMap(Branch branch) {
        if(branch == null) {
            return null;
        }
        List<BranchAppointmentInfo> infos = branch.getBranchAppointmentInfo();
        if (infos == null || infos.isEmpty()) {
            return null;
        }
        return infos.stream()
                .collect(Collectors.toMap(
                        BranchAppointmentInfo::day,
                        info -> new BranchAppointmentInfoEntity(
                                branch.getBranchId(),
                                Math.toIntExact(info.slotDuration().toMinutes()),
                                info.utilizationFactor(),
                                info.staffCount(),
                                info.day()
                        )
                ));
    }

    default List<BranchAppointmentInfo> mapAppointmentInfoToList(Map<LocalDate, BranchAppointmentInfoEntity> map) {
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

    static   Map<LocalDate, OperationHoursOverrideEntity> mapOperationHoursOverrideToMap(Branch branch) {
        if(branch == null) {
            return null;
        }
        List<OperationHoursOverride> infos = branch.getOperationHoursOverride();
        if (infos == null || infos.isEmpty()) {
            return null;
        }
        return infos.stream()
                .collect(Collectors.toMap(
                        OperationHoursOverride::effectiveDate,
                        info -> new OperationHoursOverrideEntity(
                                branch.getBranchId(),
                                info.effectiveDate(),
                                info.openTime(),
                                info.closingTime(),
                                info.closed(),
                                info.reason(),
                                LocalDateTime.now(),
                                null
                        )
                ));
    }

    default List<OperationHoursOverride> mapOperationHoursOverrideToMapToList(Map<LocalDate, OperationHoursOverrideEntity> map) {
        if (map == null || map.isEmpty()) {
            return Collections.emptyList();
        }
        return map.values()
                .stream()
                .map(operationHoursOverrideEntity -> new OperationHoursOverride(
                        operationHoursOverrideEntity.effectiveDate(),
                        operationHoursOverrideEntity.openTime(),
                        operationHoursOverrideEntity.closingTime(),
                        operationHoursOverrideEntity.closed(),
                        operationHoursOverrideEntity.reason()
                ))
                .toList();
    }

}
