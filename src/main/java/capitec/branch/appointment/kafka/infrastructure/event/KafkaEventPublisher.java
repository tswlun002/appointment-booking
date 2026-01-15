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
public class KafkaEventPublisher<K extends Serializable, V extends EventValue, E extends ErrorEventValue>
        implements EventPublisher<K, V>, CallbackEventPublisher<K, V, E> {

    private final KafkaTemplate<K, V> kafkaTemplate;
    private final ProducerProperties producerProperties;
    private final EventValueFactory eventValueFactory;

    @Override
    public CompletableFuture<PublisherResults<K, V>> publishAsync(K key, V event) {
        CompletableFuture<PublisherResults<K, V>> publisherResults;

        try {
            var future = sendToKafka(key, event);
            publisherResults = future.handleAsync((res, err) -> toPublisherResults(key, event, res, err));
        } catch (TimeoutException | KafkaException e) {
            PublisherResults<K, V> publisherResultsError = new PublisherResults<>(event, key, null, null, e);
            publisherResults = CompletableFuture.completedFuture(publisherResultsError);
        }

        return publisherResults;
    }

    private CompletableFuture<SendResult<K, V>> sendToKafka(K key, V event) {
        return getPartition(event)
                .map(partition -> kafkaTemplate.send(event.getTopic(), Math.toIntExact(partition), key, event))
                .orElseGet(() -> kafkaTemplate.send(event.getTopic(), key, event));
    }

    private Optional<Long> getPartition(V event) {
        if (event instanceof ErrorEventValue errorEventValue) {
            return Optional.ofNullable(errorEventValue.getPartition());
        }
        return Optional.empty();
    }

    private PublisherResults<K, V> toPublisherResults(K key, V event, SendResult<K, V> result, Throwable throwable) {
        if (result == null) {
            return new PublisherResults<>(event, key, null, null, throwable);
        }
        var record = result.getProducerRecord();
        var metadata = result.getRecordMetadata();
        return new PublisherResults<>(record.value(), record.key(), (long) metadata.partition(), metadata.offset(), throwable);
    }

    @Override
    public PublisherResults<K, V> publish(K key, V event) throws ExecutionException, InterruptedException {
        try {
            return publishAsync(key, event).get(producerProperties.getDeliverTimeOutMs(), TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            return new PublisherResults<>(event, key, null, null, e);
        }
    }

    @Override
    public List<PublisherResults<K, V>> publishBatch(List<KeyValue<K, V>> events) {
        try {
            return publishBatchAsync(events).get(producerProperties.getDeliverTimeOutMs(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | java.util.concurrent.TimeoutException e) {
            return events.stream()
                    .map(kv -> new PublisherResults<>(kv.value(), kv.key(), null, null, e))
                    .toList();
        }
    }

    @Override
    public CompletableFuture<List<PublisherResults<K, V>>> publishBatchAsync(List<KeyValue<K, V>> events) {
        if (events == null || events.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }

        List<CompletableFuture<PublisherResults<K, V>>> futures = events.stream()
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
    public <I extends Serializable, T extends EventValue> PublisherResults<I, T> callback(final PublisherResults<K, V> results) {
        V event = results.event();
        Throwable throwable = results.exception();

        if (throwable == null) {
            return handleSuccess(results, event);
        }
        return handleFailure(results, event, throwable);
    }

    @SuppressWarnings("unchecked")
    private <I extends Serializable, T extends EventValue> PublisherResults<I, T> handleSuccess(
            PublisherResults<K, V> results, V event) {

        if (event instanceof ErrorEventValue errorEventValue) {
            return new PublisherResults<>(
                    (T) errorEventValue,
                    (I) errorEventValue.getKey(),
                    results.partition(),
                    results.offset(),
                    null
            );
        }
        return new PublisherResults<>((T) event, (I) event.getKey(), results.partition(), results.offset(), null);
    }

    @SuppressWarnings("unchecked")
    private <I extends Serializable, T extends EventValue> PublisherResults<I, T> handleFailure(
            PublisherResults<K, V> results, V event, Throwable throwable) {

        if (event instanceof ErrorEventValue errorEventValue) {
            return new PublisherResults<>(
                    (T) errorEventValue,
                    (I) errorEventValue.getKey(),
                    errorEventValue.getPartition(),
                    errorEventValue.getOffset(),
                    throwable
            );
        }

        boolean isRetryable = isInstanceOfRetryableExceptions()
                .apply(throwable, producerProperties.getRetryableExceptions());

        DefaultErrorEventValue errorEvent = createErrorEvent(results, event, throwable, isRetryable);

        return new PublisherResults<>(
                (T) errorEvent,
                (I) errorEvent.getKey(),
                errorEvent.getPartition(),
                errorEvent.getOffset(),
                throwable
        );
    }

    private DefaultErrorEventValue createErrorEvent(
            PublisherResults<K, V> results, V event, Throwable throwable, boolean isRetryable) {

        Throwable cause = throwable.getCause();
        String exception = cause != null ? cause.getMessage() : throwable.getMessage();
        String causeClass = cause != null ? cause.getClass().getName() : throwable.getClass().getName();
        String stackTrace = (throwable.getStackTrace() != null && throwable.getStackTrace().length != 0)
                ? Arrays.toString(throwable.getStackTrace())
                : throwable.fillInStackTrace().toString();

        return (DefaultErrorEventValue) eventValueFactory.createErrorEventValue(
                event.getTopic(), event.getValue(),
                event.getTraceId(), event.getEventId(), event.getPublishTime(),
                results.partition(), results.offset(), event.getKey(),
                exception, throwable.getClass().getName(),
                causeClass, stackTrace, isRetryable
        );
    }
}
