package capitec.branch.appointment.location.infrastructure.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class BranchLocationResilienceConfig {

    public static final String BRANCH_LOCATOR_CIRCUIT_BREAKER = "branchLocatorCircuitBreaker";
    public static final String BRANCH_LOCATOR_RETRY = "branchLocatorRetry";

    private final BranchLocationResilienceProperties properties;

    @Bean
    @SuppressWarnings("unchecked")
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        var cbProps = properties.getCircuitBreaker();

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(cbProps.getFailureRateThreshold())
                .minimumNumberOfCalls(cbProps.getMinimumNumberOfCalls())
                .waitDurationInOpenState(cbProps.getWaitDurationInOpenState())
                .permittedNumberOfCallsInHalfOpenState(cbProps.getPermittedNumberOfCallsInHalfOpenState())
                .slidingWindowSize(cbProps.getSlidingWindowSize())
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .recordExceptions(cbProps.getRecordExceptions().toArray(Class[]::new))
                .ignoreExceptions(cbProps.getIgnoreExceptions().toArray(Class[]::new))
                .build();

        return CircuitBreakerRegistry.of(config);
    }

    @Bean
    public CircuitBreaker branchLocatorCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker(BRANCH_LOCATOR_CIRCUIT_BREAKER);
    }

    @Bean
    @SuppressWarnings("unchecked")
    public RetryRegistry retryRegistry() {
        var retryProps = properties.getRetry();

        RetryConfig config = RetryConfig.custom()
                .maxAttempts(retryProps.getMaxAttempts())
                .intervalFunction(IntervalFunction.ofExponentialRandomBackoff(
                        retryProps.getInitialInterval(),
                        retryProps.getMultiplier(),
                        retryProps.getRandomizationFactor()
                ))
                .retryExceptions(retryProps.getRetryExceptions().toArray(Class[]::new))
                .ignoreExceptions(retryProps.getIgnoreExceptions().toArray(Class[]::new))
                .build();

        return RetryRegistry.of(config);
    }

    @Bean
    public Retry branchLocatorRetry(RetryRegistry registry) {
        return registry.retry(BRANCH_LOCATOR_RETRY);
    }
}

