package lunga.appointmentbooking.kafka.domain;

import java.util.Set;

public interface EventListener <T extends EventValue> {

    void receive(T event) throws Exception;
    void receive(Set<T> connectEvent,Set<String> keys, Set<String> partitions, Set<Integer> offsets);
}
