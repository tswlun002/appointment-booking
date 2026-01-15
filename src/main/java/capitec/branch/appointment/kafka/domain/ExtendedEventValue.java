package capitec.branch.appointment.kafka.domain;

import java.io.Serializable;

public interface ExtendedEventValue<T extends Serializable> extends EventValue {
    T getMetadata();
}
