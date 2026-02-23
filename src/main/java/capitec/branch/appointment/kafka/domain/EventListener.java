package capitec.branch.appointment.kafka.domain;

import java.io.Serializable;
import java.util.Set;

public interface EventListener <K extends  Serializable,V extends Serializable> {

    void receive(EventValue<K,V> event) throws Exception;
    void receive(Set<EventValue<K,V>> connectEvent, Set<String> keys, Set<String> partitions, Set<Integer> offsets);
}
