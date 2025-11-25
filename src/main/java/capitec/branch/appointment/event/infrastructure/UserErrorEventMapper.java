package capitec.branch.appointment.event.infrastructure;



import capitec.branch.appointment.event.infrastructure.dao.UserErrorEventValueEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;
import capitec.branch.appointment.kafka.domain.DEAD_LETTER_STATUS;
import capitec.branch.appointment.kafka.user.UserDefaultErrorEvent;
@Component
@Mapper(componentModel = "spring")
public  interface UserErrorEventMapper {


    // === ENTITY -> DOMAIN MODEL ===
    @Mapping(source = "status.status", target = "deadLetterStatus", qualifiedByName = "mapToDeadLetterStatus")
    UserDefaultErrorEvent toModel(UserErrorEventValueEntity entity);

    // === DOMAIN MODEL -> ENTITY ===
    @Mapping(source = "deadLetterStatus", target = "status", qualifiedByName = "mapToStatus")
    UserErrorEventValueEntity toEntity(UserDefaultErrorEvent model);

 //   @Mapping(source = "deadLetterStatus", target = "status", qualifiedByName = "mapToStatus")
   default UserErrorEventValueEntity toEntityWithNullId(UserDefaultErrorEvent model){
        // First use the standard mapping
        UserErrorEventValueEntity entity = toEntity(model);

        // Then create a new instance with null ID
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
                entity.exception(),
                entity.exceptionClass(),
                entity.causeClass(),
                entity.stackTrace(),
                entity.traceId(),
                entity.status(),
                entity.publishTime(),
                entity.fullname(),
                entity.username(),
                entity.email()
        );

    }

    // === Helper: DEAD_LETTER_STATUS enum -> status record ===
    @Named("mapToStatus")
    static UserEventStatus mapToStatus(DEAD_LETTER_STATUS status) {
        return status != null ? new UserEventStatus(status.name()) : null;
    }

    // === Helper: status record -> DEAD_LETTER_STATUS enum ===
    @Named("mapToDeadLetterStatus")
    static DEAD_LETTER_STATUS mapToDeadLetterStatus(String status) {
        try {
            return status != null ? DEAD_LETTER_STATUS.valueOf(status) : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
