package capitec.branch.appointment.event.infrastructure.dao;


import capitec.branch.appointment.event.domain.ErrorEvent;
import capitec.branch.appointment.sharekernel.id.IdStore;
import jakarta.ws.rs.InternalServerErrorException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import capitec.branch.appointment.event.domain.RecordStatus;
import capitec.branch.appointment.event.domain.EventDeadLetterService;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import capitec.branch.appointment.event.domain.DEAD_LETTER_STATUS;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static capitec.branch.appointment.event.infrastructure.config.DeadLetterCacheConfig.DEAD_LETTER_CACHE;
import static capitec.branch.appointment.event.infrastructure.config.DeadLetterCacheConfig.DEAD_LETTER_CACHE_MANAGER;

@Slf4j
@RequiredArgsConstructor
@Service
@Validated
@Primary
public class EventDeadLetterServiceImpl implements EventDeadLetterService {

    private final FailedRecordRepository repository;

    private final IdStore idStore;

    private final ErrorEventMapper errorEventMapper;

    @Override
    @CachePut(value = DEAD_LETTER_CACHE, key = "#errorEventValue.eventId", cacheManager = DEAD_LETTER_CACHE_MANAGER, unless = "#errorEventValue == null")
    public void saveDeadLetter(ErrorEvent errorEventValue) {
        log.info("Saving dead letter to database. eventId: {}, traceId: {}",
                errorEventValue.getEventId(), errorEventValue.getTraceId());
        try {
            idStore.setId(errorEventValue.getEventId());
            var entity = errorEventMapper.toEntityWithNullId(errorEventValue);
            repository.save(entity);
            log.info("Dead letter saved successfully. eventId: {}, traceId: {}",
                    errorEventValue.getEventId(), errorEventValue.getTraceId());
        } catch (Exception e) {
            log.error("Failed to save dead letter. eventId: {}, traceId: {}",
                    errorEventValue.getEventId(), errorEventValue.getTraceId(), e);
            throw new InternalServerErrorException("Internal server error.", e);
        }
    }

    @Override
    public Set<ErrorEvent> findByStatus(RecordStatus recordStatus, int offset, int limit) {
        log.debug("Finding dead letters by status. status: {}, offset: {}, limit: {}",
                recordStatus, offset, limit);
        var results = repository.findAllByStatus(recordStatus.name(), offset, limit)
                .stream().map(errorEventMapper::toModel).collect(Collectors.toSet());
        log.debug("Found {} dead letters with status: {}", results.size(), recordStatus);
        return results;
    }

    @Override
    @CachePut(value = DEAD_LETTER_CACHE, key = "#errorEventValue.eventId", cacheManager = DEAD_LETTER_CACHE_MANAGER, unless = "#errorEventValue == null")
    public ErrorEvent updateStatus(ErrorEvent errorEventValue) {
        if (errorEventValue == null) {
            log.error("Cannot update status: ErrorEvent is null");
            throw new InternalServerErrorException("Invalid FailedRecord can not be updated");
        }

        log.debug("Updating dead letter status. eventId: {}, status: {}, retryCount: {}",
                errorEventValue.getEventId(), errorEventValue.getStatus(), errorEventValue.getRetryCount());
        try {
            var entity = errorEventMapper.toEntity(errorEventValue);

            UserErrorEventValueEntity save = repository.save(entity);
            return errorEventMapper.toModel(save);

        } catch (Exception e) {
            log.error("Failed to update dead letter status. eventId: {}, traceId: {}",
                    errorEventValue.getEventId(), errorEventValue.getTraceId(), e);
            throw new InternalServerErrorException("Internal server error");
        }
    }

    @Override
    public List<ErrorEvent> recoverDeadLetter(boolean isRetryable, DEAD_LETTER_STATUS status, int offset, int limit, int maxRetry) {
        log.debug("Recovering dead letters. isRetryable: {}, status: {}, offset: {}, limit: {}, maxRetry: {}",
                isRetryable, status, offset, limit, maxRetry);
        var results = repository
                .recoverDeadLetter(isRetryable, status.name(), offset, limit, maxRetry)
                .stream()
                .map(errorEventMapper::toModel)
                .toList();
        log.debug("Recovered {} dead letters for retry", results.size());
        return results;
    }

    @Override
    @Cacheable(value = DEAD_LETTER_CACHE, key = "#eventId", cacheManager = DEAD_LETTER_CACHE_MANAGER, unless = "#result == null || !#result.isPresent()")
    public Optional<ErrorEvent> findById(String eventId) {
        var result = repository.findById(eventId).map(errorEventMapper::toModel);
        if (result.isPresent()) {
            log.debug("Dead letter found. eventId: {}", eventId);
        } else {
            log.debug("Dead letter not found. eventId: {}", eventId);
        }
        return result;
    }

}