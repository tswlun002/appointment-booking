package capitec.branch.appointment.location.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "capitec.branch-locator.resilience")
public class BranchLocationResilienceProperties {

    private CircuitBreakerProperties circuitBreaker = new CircuitBreakerProperties();
    private RetryProperties retry = new RetryProperties();

    @Getter
    @Setter
    public static class CircuitBreakerProperties {
        /**
         * Failure rate threshold percentage to open the circuit
         */
        private float failureRateThreshold = 65;

        /**
         * Minimum number of calls before calculating failure rate
         */
        private int minimumNumberOfCalls = 5;

        /**
         * Time to wait in open state before transitioning to half-open
         */
        private Duration waitDurationInOpenState = Duration.ofSeconds(30);

        /**
         * Number of permitted calls in half-open state
         */
        private int permittedNumberOfCallsInHalfOpenState = 3;

        /**
         * Sliding window size for failure rate calculation
         */
        private int slidingWindowSize = 10;

        /**
         * Exceptions that count as failures
         */
        private List<Class<? extends Throwable>> recordExceptions = List.of(
                java.io.IOException.class,
                java.util.concurrent.TimeoutException.class,
                java.net.ConnectException.class,
                java.net.SocketTimeoutException.class,
                org.springframework.web.client.HttpServerErrorException.class,
                org.springframework.web.client.ResourceAccessException.class
        );

        /**
         * Exceptions that don't count as failures
         */
        private List<Class<? extends Throwable>> ignoreExceptions = List.of(
                IllegalArgumentException.class
        );
    }

    @Getter
    @Setter
    public static class RetryProperties {
        /**
         * Maximum retry attempts
         */
        private int maxAttempts = 3;

        /**
         * Initial interval for exponential backoff
         */
        private Duration initialInterval = Duration.ofMillis(300);

        /**
         * Multiplier for exponential backoff
         */
        private double multiplier = 2.0;

        /**
         * Randomization factor (jitter) for backoff - 0.3 means ±30%
         */
        private double randomizationFactor = 0.3;

        /**
         * Exceptions that trigger retry
         */
        private List<Class<? extends Throwable>> retryExceptions = List.of(
                java.io.IOException.class,
                java.util.concurrent.TimeoutException.class,
                java.net.ConnectException.class,
                java.net.SocketTimeoutException.class,
                org.springframework.web.client.HttpServerErrorException.class,
                org.springframework.web.client.ResourceAccessException.class
        );

        /**
         * Exceptions that should NOT trigger retry
         */
        private List<Class<? extends Throwable>> ignoreExceptions = List.of(
                IllegalArgumentException.class,
                io.github.resilience4j.circuitbreaker.CallNotPermittedException.class
        );
    }
}

