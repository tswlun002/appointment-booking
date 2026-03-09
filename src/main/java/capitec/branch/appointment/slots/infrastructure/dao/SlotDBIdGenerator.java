package capitec.branch.appointment.slots.infrastructure.dao;

import capitec.branch.appointment.sharekernel.id.IdStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.relational.core.mapping.event.BeforeConvertCallback;
import org.springframework.stereotype.Component;

import java.util.UUID;
@Component
@RequiredArgsConstructor
@Slf4j
class SlotDBIdGenerator implements BeforeConvertCallback<SlotEntity> {
    private final IdStore idStore;
    @Override
    public SlotEntity onBeforeConvert(SlotEntity entity) {

        if(entity.id() == null){
            log.debug("Generating new ID for SlotEntity:{}", entity);
           return new SlotEntity(UUID.fromString(idStore.getIdList().get(entity.bookingCount()-1)),entity);
        }
        log.debug("Updating ID for SlotEntity:{}", entity);
        return entity;

    }
}