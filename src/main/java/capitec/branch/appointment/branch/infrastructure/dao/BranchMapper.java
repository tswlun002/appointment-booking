package capitec.branch.appointment.branch.infrastructure.dao;

import capitec.branch.appointment.branch.domain.Branch;
import capitec.branch.appointment.branch.domain.appointmentinfo.BranchAppointmentInfo;
import capitec.branch.appointment.branch.domain.appointmentinfo.DayType;
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
    static Branch toDomain(BranchEntity entity){
        return Branch.restituteFromPersistence(entity.branchId(), entity.branchName(),
                mapAppointmentInfoToList(entity.branchAppointmentInfo()),
                mapOperationHoursOverrideToMapToList(entity.operationHoursOverride())
        );
    }

    /**
     * Converts Branch (Domain Model) to BranchEntity Record.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "updatedAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "branchAppointmentInfo", expression = "java(capitec.branch.appointment.branch.infrastructure.dao.BranchMapper.mapAppointmentInfoToMap(domain))")
    @Mapping(target = "operationHoursOverride", expression = "java(capitec.branch.appointment.branch.infrastructure.dao.BranchMapper.mapOperationHoursOverrideToMap(domain))")
    BranchEntity toEntity(Branch domain);

    static   Map<String, BranchAppointmentInfoEntity> mapAppointmentInfoToMap(Branch branch) {
        if(branch == null) {
            return null;
        }
        List<BranchAppointmentInfo> infos = branch.getBranchAppointmentInfo();
        if (infos == null || infos.isEmpty()) {
            return null;
        }
        return infos.stream()
                .collect(Collectors.toMap(
                        info -> info.day().name(),
                        info -> new BranchAppointmentInfoEntity(
                                branch.getBranchId(),
                                Math.toIntExact(info.slotDuration().toMinutes()),
                                info.utilizationFactor(),
                                info.staffCount(),
                                info.day().name(),
                                info.maxBookingCapacity()
                        )
                ));
    }

  static    List<BranchAppointmentInfo> mapAppointmentInfoToList(Map<String, BranchAppointmentInfoEntity> map) {
        if (map == null || map.isEmpty()) {
            return Collections.emptyList();
        }
        return map.entrySet().stream()
                .map(entry -> new BranchAppointmentInfo(
                        Duration.ofMinutes(entry.getValue().slotDuration()),
                        entry.getValue().utilizationFactor(),
                        entry.getValue().staffCount(),
                        DayType.valueOf(entry.getKey()),
                        entry.getValue().maxBookingCapacity()
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
                                info.openAt(),
                                info.closeAt(),
                                info.closed(),
                                info.reason(),
                                LocalDateTime.now(),
                                null
                        )
                ));
    }

    static List<OperationHoursOverride> mapOperationHoursOverrideToMapToList(Map<LocalDate, OperationHoursOverrideEntity> map) {
        if (map == null || map.isEmpty()) {
            return Collections.emptyList();
        }
        return map.values()
                .stream()
                .map(operationHoursOverrideEntity -> new OperationHoursOverride(
                        operationHoursOverrideEntity.effectiveDate(),
                        operationHoursOverrideEntity.openAt(),
                        operationHoursOverrideEntity.closeAt(),
                        operationHoursOverrideEntity.closed(),
                        operationHoursOverrideEntity.reason()
                ))
                .toList();
    }

}
