package capitec.branch.appointment.slots.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.net.SocketTimeoutException;
import java.util.Map;

@Configuration
public class RetryConfig {

    private  final SlotRetryableError slotRetryableError;

    public RetryConfig(SlotRetryableError slotRetryableError) {
        this.slotRetryableError = slotRetryableError;
    }

    @Bean
    public RetryTemplate slotGenerationRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();


        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(5, slotRetryableError.getRetryables(), true);
        retryTemplate.setRetryPolicy(retryPolicy);

        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(2000);
        backOffPolicy.setMultiplier(2.0);
        backOffPolicy.setMaxInterval(60000);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        return retryTemplate;
    }
}
