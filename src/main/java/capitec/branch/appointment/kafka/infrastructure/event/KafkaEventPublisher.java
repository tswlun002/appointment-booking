package capitec.branch.appointment.kafka.infrastructure.event;

import capitec.branch.appointment.kafka.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.TimeoutException;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import capitec.branch.appointment.kafka.infrastructure.configuration.properties.ProducerProperties;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaEventPublisher<K extends Serializable, V extends Serializable>
        implements EventPublisher<K,V>, CallbackEventPublisher<K,V> {

    private final KafkaTemplate<K, EventValue<K,V>> kafkaTemplate;
    private final ProducerProperties producerProperties;

    @Override
    public CompletableFuture<PublisherResults<K,V>> publishAsync(K key, EventValue<K,V> event) {
        CompletableFuture<PublisherResults<K,V>> publisherResults;

        try {
            var future = sendToKafka(key, event);
            publisherResults = future.handleAsync((res, err) -> toPublisherResults(key, event, res, err));
        } catch (TimeoutException | KafkaException e) {
            PublisherResults<K,V> publisherResultsError = new PublisherResults<>(event, key, null, null, e);
            publisherResults = CompletableFuture.completedFuture(publisherResultsError);
        }

        return publisherResults;
    }

    private CompletableFuture<SendResult<K, EventValue<K,V>>> sendToKafka(K key, EventValue<K,V> event) {
        return getPartition(event)
                .map(partition -> kafkaTemplate.send(event.topic(), Math.toIntExact(partition), key, event))
                .orElseGet(() -> kafkaTemplate.send(event.topic(), key, event));
    }


    private  Optional<Long> getPartition(EventValue<K,V> event) {
        return switch (event) {
            case EventValue.OriginEventValue<K,V> ignore -> Optional.empty();
            case EventValue.EventError<K,V> e ->  Optional.ofNullable(e.partition());
        };
    }

    private PublisherResults<K,V> toPublisherResults(K key, EventValue<K,V> event, SendResult<K, EventValue<K,V>> result, Throwable throwable) {
        if (result == null) {
            return new PublisherResults<>(event, key, null, null, throwable);
        }
        var record = result.getProducerRecord();
        var metadata = result.getRecordMetadata();
        return new PublisherResults<>(record.value(), record.key(), (long) metadata.partition(), metadata.offset(), throwable);
    }

    @Override
    public PublisherResults<K,V> publish(K key, EventValue<K,V> event) throws ExecutionException, InterruptedException {
        try {
            return publishAsync(key, event).get(producerProperties.getDeliverTimeOutMs(), TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            return new PublisherResults<>(event, key, null, null, e);
        }
    }

    @Override
    public List<PublisherResults<K,V>> publishBatch(List<KeyValue<K, EventValue<K,V>>> events) {
        try {
            return publishBatchAsync(events).get(producerProperties.getDeliverTimeOutMs(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | java.util.concurrent.TimeoutException e) {
            return events.stream()
                    .map(kv -> new PublisherResults<>(kv.value(), kv.key(), null, null, e))
                    .toList();
        }
    }

    @Override
    public CompletableFuture<List<PublisherResults<K,V>>> publishBatchAsync(List<KeyValue<K, EventValue<K,V>>> events) {
        if (events == null || events.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }

        List<CompletableFuture<PublisherResults<K,V>>> futures = events.stream()
                .map(kv -> publishAsync(kv.key(), kv.value()))
                .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(_ -> futures.stream()
                        .map(CompletableFuture::join)
                        .toList());
    }

    public static BiFunction<Throwable, Set<Class<? extends Exception>>, Boolean> isInstanceOfRetryableExceptions() {
        return (exception, exceptions) ->
                exceptions.stream()
                        .anyMatch(retryableException -> retryableException.isInstance(exception) ||
                                (exception.getCause() != null && retryableException.isInstance(exception.getCause())));
    }

    @Override
    public  PublisherResults<K,V> callback(final PublisherResults<K,V> results) {
        EventValue<K,V> event = results.event();
        Throwable throwable = results.exception();

        if (throwable == null) {
            return handleSuccess(results, event);
        }
        return handleFailure(results, event, throwable);
    }

    @SuppressWarnings("unchecked")
    private PublisherResults<K,V> handleSuccess(PublisherResults<K,V> results, EventValue<K,V> event) {

       return switch (event) {
            case EventValue.OriginEventValue<K,V> ori -> new PublisherResults<>(ori, (K)ori.key(), results.partition(), results.offset(), null);
            case EventValue.EventError<K,V> err -> new PublisherResults<>(err,(K)err.key(), results.partition(), results.offset(), null);
        };
    }

    private  PublisherResults<K,V> handleFailure(PublisherResults<K,V> results, EventValue<K,V> event, Throwable throwable) {

        return switch (event){
            case EventValue.OriginEventValue<K,V> originEventValue->{

                boolean isRetryable = isInstanceOfRetryableExceptions()
                        .apply(throwable, producerProperties.getRetryableExceptions());

                EventValue.EventError<K,V> errorEvent = createErrorEvent(results, originEventValue, throwable, isRetryable);

                yield new PublisherResults<>(
                        errorEvent,
                        errorEvent.key(),
                        errorEvent.partition(),
                        errorEvent.offset(),
                        throwable
                );
            }
            case EventValue.EventError<K,V> err -> new PublisherResults<>(
                     err,
                    err.key(),
                    err.partition(),
                    err.offset(),
                    throwable
            );
        };



    }

    private EventValue.EventError<K,V> createErrorEvent(
            PublisherResults<K,V> results, EventValue.OriginEventValue<K,V> event, Throwable throwable, boolean isRetryable) {

        Throwable cause = throwable.getCause();
        String exception = cause != null ? cause.getMessage() : throwable.getMessage();
        String causeClass = cause != null ? cause.getClass().getName() : throwable.getClass().getName();
        String stackTrace = (throwable.getStackTrace() != null && throwable.getStackTrace().length != 0)
                ? Arrays.toString(throwable.getStackTrace())
                : throwable.fillInStackTrace().toString();

        return new EventValue.EventError<>(
                event.key(),
                event.value(),
                event.traceId(),
                event.topic(),
                event.eventId(),
                event.publishTime(),
                results.partition(),
                results.offset(),
                exception,
                throwable.getClass().getName(),
                causeClass,
                stackTrace,
                isRetryable
        );
    }
}
