package capitec.branch.appointment.kafka.app;

import capitec.branch.appointment.kafka.domain.EventValue;
import capitec.branch.appointment.kafka.domain.KeyValue;
import capitec.branch.appointment.kafka.domain.PublisherResults;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public interface EventPublishUseCase {
    CompletableFuture<Boolean> publishEventAsync(EventValue event) ;
    CompletableFuture<Map<String, Boolean>> publishBatchAsync(List<EventValue> events);

    Function<PublisherResults<Serializable, EventValue>, Boolean> callback();
}
