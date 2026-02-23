package capitec.branch.appointment.kafka.domain;

import java.io.Serializable;

public record PublisherResults<K extends Serializable, V extends Serializable>(
        EventValue<K,V> event, K key, Long partition, Long offset, Throwable exception
) {
}
