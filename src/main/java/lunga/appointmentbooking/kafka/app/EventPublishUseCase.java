package lunga.appointmentbooking.kafka.app;

import lunga.appointmentbooking.kafka.domain.EventValue;
import lunga.appointmentbooking.kafka.domain.PublisherResults;

import java.io.Serializable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@UseCase
public interface EventPublishUseCase {
    CompletableFuture<Boolean> publishEventAsync(EventValue event) ;

    Function<PublisherResults<Serializable, EventValue>, Boolean> callback();
}
