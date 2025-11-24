package lunga.appointmentbooking.kafka.domain;



import java.io.Serializable;


public interface CallbackEventPublisher<K extends Serializable,V extends EventValue,E extends ErrorEventValue> {

 <I extends Serializable,T extends EventValue> PublisherResults<I,T> callback(final PublisherResults<K,V> results);
}
