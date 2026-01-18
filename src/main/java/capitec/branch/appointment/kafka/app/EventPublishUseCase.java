package capitec.branch.appointment.kafka.app;

import capitec.branch.appointment.kafka.domain.EventValue;
import capitec.branch.appointment.kafka.domain.PublisherResults;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public interface EventPublishUseCase<K extends  Serializable, V extends  Serializable> {
    CompletableFuture<Boolean> publishEventAsync(EventValue<K,V> event) ;
    CompletableFuture<Map<K, Boolean>> publishBatchAsync(List<EventValue<K,V>> events);

    Function<PublisherResults<K,V>, Boolean> callback();
}
