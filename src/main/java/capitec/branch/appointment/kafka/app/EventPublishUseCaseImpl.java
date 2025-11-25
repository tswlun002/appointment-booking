package capitec.branch.appointment.kafka.app;

import capitec.branch.appointment.kafka.domain.*;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import capitec.branch.appointment.kafka.domain.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@Slf4j

@ConditionalOnProperty(value = "kafka.event-publisher-default-impl.enabled", havingValue = "true")
@RequiredArgsConstructor
@UseCase
public class EventPublishUseCaseImpl implements EventPublishUseCase {

    private final EventPublisher<String, EventValue> eventPublisher;
    private final CallbackEventPublisher<String, EventValue, ErrorEventValue> callbackEventPublisher;
    private final DeadLetterService<ErrorEventValue> deadLetterService;

    @Override
    public CompletableFuture<Boolean> publishEventAsync(EventValue event) {

        return eventPublisher.publishAsync(event.getKey(), event)
                .handle((results, throwable) ->

                        callbackEventPublisher.callback(results))
                .thenApplyAsync(callback());

    }

    @Override
    public Function<PublisherResults<Serializable, EventValue>, Boolean> callback() {

        return results -> {

            var isSaved = false;

            var event = results.event();

            try {

                if(event instanceof ErrorEventValue errorEventValue){

                    switch (errorEventValue.getDeadLetterStatus()){

                        case DEAD ->  deadLetterService.saveDeadLetter(errorEventValue);
                        case RECOVERED ->   deadLetterService.updateStatus(errorEventValue);
                    }

                    isSaved=errorEventValue.isRetryable();
                }
                else {
                    isSaved = true;
                }

            } catch (Exception e) {

                log.warn("Failed to save dead letter:{} to database, traceId:{}", event.getEventId(), event.getTraceId(), e);
                throw new RuntimeException(e);

            }

            return isSaved;
        };

    }


    public static DefaultEventValue AnonymousDefaultEventValue(@NonNull String topic, String value, String traceId){

        return  new DefaultEventValue(topic, value, traceId){
            @Override
            public String getTopic() {
                return super.getTopic();
            }

            @Override
            public String getValue() {
                return super.getValue();
            }

            @Override
            public String getTraceId() {
                return super.getTraceId();
            }

            @Override
            public String getEventId() {
                return super.getEventId();
            }

            @Override
            public LocalDateTime getPublishTime() {
                return super.getPublishTime();
            }

            @Override
            public String toString() {
                return super.toString();
            }

            @Override
            public String getKey() {
                return super.getKey();
            }
        };
    }

    public static DefaultErrorEventValue AnonymousDefaultErrorValue(
            @NonNull String topic, String event, String traceId, String eventId, LocalDateTime publishTime,
            Long partition, Long offset, String key, String exception, String exceptionClass,
            String causeClass, String stackTrace, boolean retryable, int retryCount, DEAD_LETTER_STATUS deadLetterStatus
    ){
        return new DefaultErrorEventValue(topic, event, traceId, eventId, publishTime, partition, offset, key, exception,
                exceptionClass, causeClass,stackTrace,retryable,retryCount,deadLetterStatus){

            @Override
            public @NonNull String getTopic() {
                return super.getTopic();
            }

            @Override
            public String getValue() {
                return super.getValue();
            }

            @Override
            public String getTraceId() {
                return super.getTraceId();
            }

            @Override
            public String getEventId() {
                return super.getEventId();
            }

            @Override
            public LocalDateTime getPublishTime() {
                return super.getPublishTime();
            }

            @Override
            public Long getPartition() {
                return super.getPartition();
            }

            @Override
            public Long getOffset() {
                return super.getOffset();
            }

            @Override
            public String getHeaders() {
                return super.getHeaders();
            }

            @Override
            public String getException() {
                return super.getException();
            }

            @Override
            public String getExceptionClass() {
                return super.getExceptionClass();
            }

            @Override
            public String getCauseClass() {
                return super.getCauseClass();
            }

            @Override
            public String getStackTrace() {
                return super.getStackTrace();
            }

            @Override
            public boolean isRetryable() {
                return super.isRetryable();
            }

            @Override
            public int getRetryCount() {
                return super.getRetryCount();
            }

            @Override
            public DEAD_LETTER_STATUS getDeadLetterStatus() {
                return super.getDeadLetterStatus();
            }

            @Override
            public void setTopic(@NonNull String topic) {
                super.setTopic(topic);
            }

            @Override
            public void setValue(String value) {
                super.setValue(value);
            }

            @Override
            public void setTraceId(String traceId) {
                super.setTraceId(traceId);
            }

            @Override
            public void setEventId(String eventId) {
                super.setEventId(eventId);
            }

            @Override
            public void setPublishTime(LocalDateTime publishTime) {
                super.setPublishTime(publishTime);
            }

            @Override
            public void setPartition(Long partition) {
                super.setPartition(partition);
            }

            @Override
            public void setOffset(Long offset) {
                super.setOffset(offset);
            }

            @Override
            public void setKey(String key) {
                super.setKey(key);
            }

            @Override
            public void setHeaders(String headers) {
                super.setHeaders(headers);
            }

            @Override
            public void setException(String exception) {
                super.setException(exception);
            }

            @Override
            public void setExceptionClass(String exceptionClass) {
                super.setExceptionClass(exceptionClass);
            }

            @Override
            public void setCauseClass(String causeClass) {
                super.setCauseClass(causeClass);
            }

            @Override
            public void setStackTrace(String stackTrace) {
                super.setStackTrace(stackTrace);
            }

            @Override
            public void setRetryable(boolean retryable) {
                super.setRetryable(retryable);
            }

            @Override
            public void setRetryCount(int retryCount) {
                super.setRetryCount(retryCount);
            }

            @Override
            public void setDeadLetterStatus(DEAD_LETTER_STATUS deadLetterStatus) {
                super.setDeadLetterStatus(deadLetterStatus);
            }

            @Override
            public String getKey() {
                return super.getKey();
            }


        };
    }

}
