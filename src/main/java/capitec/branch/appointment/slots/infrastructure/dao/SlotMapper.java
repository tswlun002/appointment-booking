package capitec.branch.appointment.slots.infrastructure.dao;

import capitec.branch.appointment.slots.domain.Slot;
import capitec.branch.appointment.slots.domain.SlotStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.UUID;

@Mapper(componentModel = "spring")
interface SlotMapper {


     Logger log = LoggerFactory.getLogger(SlotMapper.class);
    default Slot toDomain(SlotEntity entity){
        return  Slot.reconstituteFromPersistence(
                entity.id(),
                entity.day(),
                entity.startTime(),
                entity.endTime(),
                entity.maxBookingCapacity(),
                entity.bookingCount(),
                entity.branchId(),
                entity.status()==null?null:SlotStatus.valueOf(entity.status()),
                entity.version()
                );
    }


    @Mapping(target = "status", source = "status", qualifiedByName = "mapSlotStatusToString")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    SlotEntity toEntity(Slot domain);

    /**
     * Maps the String status from the entity to the SlotStatus enum in the domain.
     */
    @Named("mapStringToSlotStatus")
    default SlotStatus mapStringToSlotStatus(String status) {
        if (status == null) return null;
        return SlotStatus.valueOf(status);
    }

    /**
     * Maps the SlotStatus enum from the domain to the String status in the entity.
     */
    @Named("mapSlotStatusToString")
    default String mapSlotStatusToString(SlotStatus status) {
        if (status == null) return null;
        return status.name();
    }
}