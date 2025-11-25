package capitec.branch.appointment.kafka.domain;

import jakarta.validation.Valid;

import java.io.Serializable;
import java.util.concurrent.CompletableFuture;

public interface EventPublisher<K extends Serializable, V extends EventValue> {

    CompletableFuture<PublisherResults<K,V>> publishAsync (K key,@Valid V value);
}
