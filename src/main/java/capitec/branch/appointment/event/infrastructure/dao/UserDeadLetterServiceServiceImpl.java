package capitec.branch.appointment.event.infrastructure.dao;


import capitec.branch.appointment.event.domain.ErrorEvent;
import capitec.branch.appointment.utils.sharekernel.id.IdStore;
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

    private final ErrorEventMapper errorEventMapper;

    @Override
    public void saveDeadLetter( ErrorEvent errorEventValue) {

        try {
                idStore.setId(errorEventValue.getEventId());
                var entity = errorEventMapper.toEntityWithNullId(errorEventValue);
                repository.save(entity);

                log.info("Saved FailedRecord, traceId: {}",errorEventValue.getTraceId());

        } catch (Exception e) {

            log.error("Internal server error. traceId: {}", errorEventValue.getTraceId(), e);
            throw new InternalServerErrorException("Internal server error.",e);
        }
    }

    @Override
    public Set<ErrorEvent> findByStatus(RecordStatus recordStatus, int offset, int limit) {

        return repository.findAllByStatus(recordStatus.name(), offset, limit)
                .stream().map(errorEventMapper::toModel).collect(Collectors.toSet());
    }

    @Override
    public void updateStatus( ErrorEvent errorEventValue) {

        if (errorEventValue == null) throw new InternalServerErrorException("Invalid FailedRecord can not be updated");


        try {

            var entity = errorEventMapper.toEntity( errorEventValue);

            repository.save(entity);

            log.info("Updated failed record status, record: {}, traceId: {}", errorEventValue, errorEventValue.getValue());

        } catch (Exception e) {

            log.error("Internal server error:{}, traceId: {}", e.getMessage(), errorEventValue.getTraceId(), e);
            throw new InternalServerErrorException("Internal server error");
        }
    }


    @Override
    public List<ErrorEvent> recoverDeadLetter(boolean isRetryable, DEAD_LETTER_STATUS status, int offset, int limit, int maxRetry) {
        return repository
                .recoverDeadLetter(isRetryable, status.name(), offset,limit, maxRetry)
                .stream()
                .map(errorEventMapper::toModel)
                .toList();
    }

    @Override
    public Optional<ErrorEvent> findById(String eventId) {
        return repository.findById(eventId).map(errorEventMapper::toModel);
    }



}