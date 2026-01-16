package capitec.branch.appointment.slots.infrastructure.dao;

import capitec.branch.appointment.utils.sharekernel.id.IdStore;
import lombok.RequiredArgsConstructor;
import org.springframework.data.relational.core.mapping.event.BeforeConvertCallback;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
class SlotDBIdGenerator implements BeforeConvertCallback<SlotEntity> {
    private final IdStore idStore;
    @Override
    public SlotEntity onBeforeConvert(SlotEntity entity) {

        if(entity.id() == null){
           return new SlotEntity(UUID.fromString(idStore.getIdList().get(entity.bookingCount()-1)),entity);
        }
        return entity;

    }
}