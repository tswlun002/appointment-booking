package capitec.branch.appointment.kafka.app;

import capitec.branch.appointment.kafka.domain.EventValue;
import capitec.branch.appointment.kafka.domain.PublisherResults;

import java.io.Serializable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@UseCase
public interface EventPublishUseCase {
    CompletableFuture<Boolean> publishEventAsync(EventValue event) ;

    Function<PublisherResults<Serializable, EventValue>, Boolean> callback();
}
