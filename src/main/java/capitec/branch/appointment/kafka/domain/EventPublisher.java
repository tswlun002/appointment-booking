package capitec.branch.appointment.kafka.domain;

import jakarta.validation.Valid;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public interface EventPublisher<K extends Serializable, V extends EventValue> {

    CompletableFuture<PublisherResults<K, V>> publishAsync(K key, @Valid V value);
    PublisherResults<K, V> publish(K key, @Valid V value) throws ExecutionException, InterruptedException;
    List<PublisherResults<K, V>> publishBatch(List<KeyValue<K, V>> events);

    CompletableFuture<List<PublisherResults<K, V>>> publishBatchAsync(List<KeyValue<K, V>> events);
}
