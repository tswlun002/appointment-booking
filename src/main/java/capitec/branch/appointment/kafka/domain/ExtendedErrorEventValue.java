package capitec.branch.appointment.kafka.domain;

public interface ExtendedErrorEventValue<T> extends ErrorEventValue {
    T getMetadata();
}