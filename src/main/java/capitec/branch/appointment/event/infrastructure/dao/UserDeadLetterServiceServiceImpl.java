package capitec.branch.appointment.event.infrastructure.dao;


import capitec.branch.appointment.event.domain.UserErrorEvent;
import capitec.branch.appointment.utils.IdStore;
import jakarta.ws.rs.InternalServerErrorException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import capitec.branch.appointment.event.domain.RecordStatus;
import capitec.branch.appointment.event.domain.UserDeadLetterService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import capitec.branch.appointment.event.domain.DEAD_LETTER_STATUS;

import java.util.List;
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
    public void saveDeadLetter( UserErrorEvent errorEventValue) {

        try {
                idStore.setId(errorEventValue.getEventId());
                var entity = userErrorEventMapper.toEntityWithNullId(errorEventValue);
                repository.save(entity);

                log.info("Saved FailedRecord, traceId: {}",errorEventValue.getTraceId());

        } catch (Exception e) {

            log.error("Internal server error. traceId: {}", errorEventValue.getTraceId(), e);
            throw new InternalServerErrorException("Internal server error.",e);
        }
    }

    @Override
    public Set<UserErrorEvent> findByStatus(RecordStatus recordStatus, int offset, int limit) {

        return repository.findAllByStatus(recordStatus.name(), offset, limit)
                .stream().map(userErrorEventMapper::toModel).collect(Collectors.toSet());
    }

    @Override
    public void updateStatus( UserErrorEvent errorEventValue) {

        if (errorEventValue == null) throw new InternalServerErrorException("Invalid FailedRecord can not be updated");


        try {

            var entity = userErrorEventMapper.toEntity( errorEventValue);

            repository.save(entity);

            log.info("Updated failed record status, record: {}, traceId: {}", errorEventValue, errorEventValue.getValue());

        } catch (Exception e) {

            log.error("Internal server error:{}, traceId: {}", e.getMessage(), errorEventValue.getTraceId(), e);
            throw new InternalServerErrorException("Internal server error");
        }
    }


    @Override
    public List<UserErrorEvent> recoverDeadLetter(boolean isRetryable, DEAD_LETTER_STATUS status, int offset, int limit, int maxRetry) {
        return repository
                .recoverDeadLetter(isRetryable, status.name(), offset,limit, maxRetry)
                .stream()
                .map(userErrorEventMapper::toModel)
                .toList();
    }

    @Override
    public Optional<UserErrorEvent> findById(String eventId) {
        return repository.findById(eventId).map(userErrorEventMapper::toModel);
    }



}