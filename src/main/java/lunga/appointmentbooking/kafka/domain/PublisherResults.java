package lunga.appointmentbooking.kafka.domain;

import java.io.Serializable;

public record PublisherResults<K extends Serializable,T extends EventValue>(
        T event,K key,Long partition,Long offset,Throwable exception
) {
}
