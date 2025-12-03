package capitec.branch.appointment.slots.infrastructure.dao;

import capitec.branch.appointment.slots.domain.Slot;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SlotMapper {

    /**
     * Converts a SlotEntity  to a Slot (Domain Model)
     */
    Slot toDomain(SlotEntity entity);

    /**
     * Converts a Slot (Domain Model) to a SlotEntity.
     */
    @Mapping(target = "id", ignore = true) // Ignore fields managed by the database
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    SlotEntity toEntity(Slot domain);
}