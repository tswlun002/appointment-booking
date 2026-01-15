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
public class EventPublishUseCaseImpl implements EventPublishUseCase {

    private final EventPublisher<String, EventValue> eventPublisher;
    private final CallbackEventPublisher<String, EventValue, ErrorEventValue> callbackEventPublisher;
    private final DeadLetterService< ErrorEventValue> deadLetterService;

    @Override
    public CompletableFuture<Boolean> publishEventAsync(EventValue event) {
        if (event == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Event cannot be null"));
        }

        return eventPublisher.publishAsync(event.getKey(), event)
                .handle((results, _) -> callbackEventPublisher.callback(results))
                .thenApplyAsync(callback());
    }

    @Override
    public CompletableFuture<Map<String, Boolean>> publishBatchAsync(List<EventValue> events) {

        if (events == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Events cannot be null"));
        }
        var listEvents = events.stream().map(s -> new KeyValue<>(s.getKey(), s)).toList();


        return eventPublisher.publishBatchAsync(listEvents)
                .handle((resultsList,_) ->
                    resultsList.stream().map(callbackEventPublisher::callback).toList()
                )
                .thenApplyAsync(results->{

                    Map<String, Boolean>  map = new HashMap<>();
                    for (PublisherResults<Serializable, EventValue> result : results) {
                        Boolean apply = callback().apply(result);
                        map.put(result.event().getEventId(), apply);
                    }
                    return map;

                });
    }

    @Override
    public Function<PublisherResults<Serializable, EventValue>, Boolean> callback() {
        return this::handleCallback;
    }

    private Boolean handleCallback(PublisherResults<Serializable, EventValue> results) {
        var event = results.event();

        try {
            if (event instanceof ErrorEventValue errorEventValue) {
                if (results.exception() == null) {
                    // Success - mark as recovered
                    deadLetterService.markRecovered(errorEventValue, results.partition(), results.offset());
                    return true;
                } else {
                    // Failure - increment retry or mark failed
                    deadLetterService.handleRetryFailure(errorEventValue);
                    return errorEventValue.isRetryable();
                }
            }
            return true;
        } catch (Exception e) {
            log.warn("Failed to save dead letter:{} to database, traceId:{}",
                    event.getEventId(), event.getTraceId(), e);
            throw new DeadLetterPersistenceException(e);
        }
    }

}
