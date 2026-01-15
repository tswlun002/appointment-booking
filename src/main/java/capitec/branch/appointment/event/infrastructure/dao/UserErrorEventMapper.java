package capitec.branch.appointment.event.infrastructure.dao;



import capitec.branch.appointment.event.domain.UserErrorEvent;
import capitec.branch.appointment.event.infrastructure.UserEventStatus;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import capitec.branch.appointment.event.domain.DEAD_LETTER_STATUS;


@Mapper(componentModel = "spring")
 interface UserErrorEventMapper {

    default UserErrorEvent toModel(UserErrorEventValueEntity entity){
        String value = entity.status().status();
        DEAD_LETTER_STATUS status = StringUtils.isBlank(value) ?null:DEAD_LETTER_STATUS.valueOf(value);
        return  UserErrorEvent.reconstitute(
                entity.eventId(),
                entity.key(),
                entity.topic(),
                entity.value(),
                entity.traceId(),
                entity.publishTime(),
                entity.exception(),
                entity.exceptionClass(),
                entity.exceptionCause(),
                entity.stackTrace(),
                entity.retryCount(),
                entity.retryable(),
                entity.nextRetryAt(),
                status,
                entity.partition(),
                entity.offset(),
                entity.fullname(),
                entity.username(),
                entity.email()
        );
    }

    @Mapping(source = "status", target = "status", qualifiedByName = "mapToStatus")
    UserErrorEventValueEntity toEntity(UserErrorEvent model);

   default UserErrorEventValueEntity toEntityWithNullId(UserErrorEvent model){
        UserErrorEventValueEntity entity = toEntity(model);
        return new UserErrorEventValueEntity(
                null, // explicitly set ID to null
                entity.key(),
                entity.value(),
                entity.topic(),
                entity.partition(),
                entity.offset(),
                entity.headers(),
                entity.retryable(),
                entity.retryCount(),
                entity.nextRetryAt(),
                entity.exception(),
                entity.exceptionClass(),
                entity.exceptionCause(),
                entity.stackTrace(),
                entity.traceId(),
                entity.status(),
                entity.publishTime(),
                entity.fullname(),
                entity.username(),
                entity.email()
        );
    }

    @Named("mapToStatus")
    static UserEventStatus mapToStatus(DEAD_LETTER_STATUS status) {
        return status != null ? new UserEventStatus(status.name()) : null;
    }
    @Named("mapToDeadLetterStatus")
    static DEAD_LETTER_STATUS mapToDeadLetterStatus(String status) {
        try {
            return status != null ? DEAD_LETTER_STATUS.valueOf(status) : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
