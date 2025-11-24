package lunga.appointmentbooking.event.app;

import lunga.appointmentbooking.kafka.domain.ErrorEventValue;

import java.util.concurrent.CompletableFuture;

public interface RetryEventPublisherSchedulerService {
    <T extends ErrorEventValue> CompletableFuture<Boolean> republishEvent(T event);
}
