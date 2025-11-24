package lunga.appointmentbooking.kafka.domain;

import org.springframework.scheduling.annotation.Scheduled;

public interface RetryEventPublisher {
    @Scheduled(fixedDelayString = "#{schedulerProperties.getFixedRateMS()}")
    void retryDeadLetter();
}
