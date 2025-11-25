package capitec.branch.appointment.event.app;

import capitec.branch.appointment.kafka.domain.ErrorEventValue;

import java.util.concurrent.CompletableFuture;

public interface RetryEventPublisherSchedulerService {
    <T extends ErrorEventValue> CompletableFuture<Boolean> republishEvent(T event);
}
