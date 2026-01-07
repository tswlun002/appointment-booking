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

        try {
            UUID id = UUID.fromString(entity.id());

            Field idField = Slot.class.getDeclaredField(Slot.ID_FIELD_NAME);
            Field statusField = Slot.class.getDeclaredField(Slot.STATUS_FIELD_NAME);
            Field versionField = Slot.class.getDeclaredField(Slot.VERSION_FIELD_NAME);

            idField.setAccessible(true);
            statusField.setAccessible(true);
            versionField.setAccessible(true);

            Slot slot = new Slot(entity.day(), entity.startTime(), entity.endTime(), entity.number(), entity.branchId());

            idField.set(slot, id);
            statusField.set(slot,SlotStatus.valueOf(entity.status()));
            versionField.set(slot,entity.version());

            return slot;

        } catch (NoSuchFieldException e) {
            log.error("Slot class does not contain an  field for reflection.", e);
            throw new RuntimeException("Mapping error: Cannot find  field for reflection.", e);
        } catch (IllegalAccessException e) {
            log.error("Failed to set field via reflection.", e);
            throw new RuntimeException("Mapping error: Access denied during reflection set.", e);
        }

    }


    @Mapping(target = "id", source = "id", qualifiedByName = "mapUUIDToString")
    @Mapping(target = "status", source = "status", qualifiedByName = "mapSlotStatusToString")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    SlotEntity toEntity(Slot domain);


    /**
     * Maps the String ID from the entity to the UUID ID in the domain.
     */
    @Named("mapStringToUUID")
    default UUID mapStringToUUID(String id) {
        if (id == null) return null;
        return UUID.fromString(id);
    }

    /**
     * Maps the UUID ID from the domain to the String ID in the entity.
     */
    @Named("mapUUIDToString")
    default String mapUUIDToString(UUID id) {
        if (id == null) return null;
        return id.toString();
    }

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