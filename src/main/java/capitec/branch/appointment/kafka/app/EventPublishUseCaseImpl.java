package capitec.branch.appointment.kafka.app;

import capitec.branch.appointment.kafka.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@Slf4j
//@ConditionalOnProperty(value = "kafka.event-publisher-default-impl.enabled", havingValue = "true")
@RequiredArgsConstructor
@UseCase
public class EventPublishUseCaseImpl<K extends  Serializable, V extends Serializable> implements EventPublishUseCase<K,V> {

    private final EventPublisher<K,V> eventPublisher;
    private final CallbackEventPublisher<K,V> callbackEventPublisher;
    private final DeadLetterService<K,V> deadLetterService;

    @Override
    public CompletableFuture<Boolean> publishEventAsync(EventValue<K,V> event) {
        if (event == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Event cannot be null"));
        }

        return eventPublisher.publishAsync( event.key(), event)
                .handle((results, _) -> callbackEventPublisher.callback(results))
                .thenApplyAsync(callback());
    }

    @Override
    public CompletableFuture<Map<K, Boolean>> publishBatchAsync(List<EventValue<K,V>> events) {

        if (events == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Events cannot be null"));
        }
        var listEvents = events.stream().map(s -> new KeyValue<>(s.key(), s)).toList();


        return eventPublisher.publishBatchAsync(listEvents)
                .handle((resultsList,_) ->
                    resultsList.stream().map(callbackEventPublisher::callback).toList()
                )
                .thenApplyAsync(results->{

                    Map<K, Boolean>  map = new HashMap<>();
                    for (var result : results) {
                        Boolean apply = callback().apply(result);
                        map.put(result.event().key(), apply);
                    }
                    return map;

                });
    }

    @Override
    public Function<PublisherResults<K,V>, Boolean> callback() {
        return this::handleCallback;
    }

    private Boolean handleCallback(PublisherResults<K,V> results) {
        var event = results.event();

        try {
            if (event instanceof EventValue.EventError<K,V> error) {
                if (results.exception() == null) {
                    // Success - mark as recovered
                    deadLetterService.markRecovered(error, results.partition(), results.offset());
                    return true;
                } else {
                    // Failure - increment retry or mark failed
                    deadLetterService.handleRetryFailure(error);
                    return error.isRetryable();
                }
            }
            return true;
        } catch (Exception e) {
            log.warn("Failed to save dead letter:{} to database, traceId:{}",
                    event.eventId(), event.traceId(), e);
            throw new DeadLetterPersistenceException(e);
        }
    }

}
