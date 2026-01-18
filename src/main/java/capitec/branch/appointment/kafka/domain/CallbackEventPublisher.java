package capitec.branch.appointment.kafka.domain;



import java.io.Serializable;


public interface CallbackEventPublisher<K extends Serializable, V extends Serializable> {

 PublisherResults<K,V> callback(final PublisherResults<K,V> results);
}
