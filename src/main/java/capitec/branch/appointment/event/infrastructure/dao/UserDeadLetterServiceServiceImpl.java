package capitec.branch.appointment.event.infrastructure.dao;


import capitec.branch.appointment.utils.IdStore;
import jakarta.ws.rs.InternalServerErrorException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import capitec.branch.appointment.event.domain.RecordStatus;
import capitec.branch.appointment.event.domain.UserDeadLetterService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import capitec.branch.appointment.kafka.domain.DEAD_LETTER_STATUS;
import capitec.branch.appointment.kafka.domain.ErrorEventValue;
import capitec.branch.appointment.kafka.user.UserDefaultErrorEvent;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
@Validated
@Primary
public class UserDeadLetterServiceServiceImpl implements UserDeadLetterService {

    private final FailedRecordRepository repository;

    private final IdStore idStore;

    private final UserErrorEventMapper userErrorEventMapper;

    @Override
    public void saveDeadLetter( ErrorEventValue errorEventValue) {

        /*if (StringUtils.isBlank(errorEventValue.getKey()) || errorEventValue.getDeadLetterStatus() == null) {

            log.error("Invalid FailedRecord details. key: {}, topic:{}, status:{}, traceId: {}", errorEventValue.getKey(), errorEventValue.getTopic(), errorEventValue.getDeadLetterStatus(), errorEventValue.getTraceId());
            throw new InternalServerErrorException("Invalid FailedRecord details.");
        }*/

        try {

            //if (errorEventValue.getEventId() == null || repository.findByEventId(errorEventValue.getEventId()).isEmpty()) {
                idStore.setId(errorEventValue.getEventId());
                UserErrorEventValueEntity entity = userErrorEventMapper.toEntityWithNullId((UserDefaultErrorEvent) errorEventValue);
                repository.save(entity);

                log.info("Saved FailedRecord, traceId: {}",errorEventValue.getTraceId());
           // }

        } catch (Exception e) {

            log.error("Internal server error. traceId: {}", errorEventValue.getTraceId(), e);
            throw new InternalServerErrorException("Internal server error.");
        }
    }

    @Override
    public Set<ErrorEventValue> findByStatus(RecordStatus recordStatus) {

        return repository.findAllByStatus(recordStatus.name()).stream().map(userErrorEventMapper::toModel).collect(Collectors.toSet());
    }

    @Override
    public void updateStatus( ErrorEventValue errorEventValue) {

        if (errorEventValue == null) throw new InternalServerErrorException("Invalid FailedRecord can not be updated");


        try {

            UserErrorEventValueEntity entity = userErrorEventMapper.toEntity((UserDefaultErrorEvent) errorEventValue);

            repository.save(entity);

            log.info("Updated failed record status, record: {}, traceId: {}", errorEventValue, errorEventValue.getValue());

        } catch (Exception e) {

            log.error("Internal server error:{}, traceId: {}", e.getMessage(), errorEventValue.getTraceId(), e);
            throw new InternalServerErrorException("Internal server error");
        }
    }



    @Override
    public Set< ErrorEventValue> recoverDeadLetter(boolean isRetryable, DEAD_LETTER_STATUS status) {
        return repository
                .recoverDeadLetter(isRetryable, status.name())
                .stream()
                .map(userErrorEventMapper::toModel)
                .collect(Collectors.toSet());
    }

    @Override
    public Optional<ErrorEventValue> findById(String eventId) {
        return repository.
                findById(eventId)
                .map(userErrorEventMapper::toModel);
    }


}