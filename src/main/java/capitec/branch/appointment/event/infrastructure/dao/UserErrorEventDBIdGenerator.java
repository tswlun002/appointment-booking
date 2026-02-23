package capitec.branch.appointment.event.infrastructure.dao;

import capitec.branch.appointment.sharekernel.id.IdStore;
import lombok.RequiredArgsConstructor;
import org.springframework.data.relational.core.mapping.event.BeforeConvertCallback;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserErrorEventDBIdGenerator implements BeforeConvertCallback<UserErrorEventValueEntity> {
    private final IdStore idStore;
    @Override
    public UserErrorEventValueEntity onBeforeConvert(UserErrorEventValueEntity entity) {

        if(entity.eventId() == null){
           return new UserErrorEventValueEntity(idStore.getId(),entity);
        }
        return entity;

    }
}
