package capitec.branch.appointment.kafka.infrastructure.configuration;






import capitec.branch.appointment.kafka.domain.EventValue;
import capitec.branch.appointment.kafka.infrastructure.configuration.properties.*;
import capitec.branch.appointment.kafka.infrastructure.configuration.properties.ConsumerProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.*;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.FixedBackOff;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import static capitec.branch.appointment.kafka.infrastructure.event.KafkaEventPublisher.isInstanceOfRetryableExceptions;

@Configuration
@EnableKafka
@Slf4j
@EnableConfigurationProperties({ConsumerProperties.class, ProducerProperties.class, SchedulerProperties.class})
public class KafkaConfig<K extends Serializable, V extends Serializable> {

    private final KafkaProperties kafkaProperties;
    private final ConsumerProperties consumerProperties;
    private final SecurityProperties securityProperties;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final Predicate<Exception> isRetryable;
    private final ProducerFactory<K, V> producerFactory;

    public KafkaConfig(KafkaProperties kafkaProperties, ConsumerProperties consumerProperties
            , SecurityProperties securityProperties, ApplicationEventPublisher applicationEventPublisher,
                       ProducerFactory<K, V> producerFactory,
                       ProducerProperties producerProperties) {
        this.kafkaProperties = kafkaProperties;
        this.consumerProperties = consumerProperties;
        this.producerFactory = producerFactory;
        this.securityProperties = securityProperties;
        this.applicationEventPublisher = applicationEventPublisher;
        isRetryable = exception -> isInstanceOfRetryableExceptions().apply(exception, consumerProperties.getRetryableExceptions()) ||
                isInstanceOfRetryableExceptions().apply(exception, producerProperties.getRetryableExceptions());
    }


    @Bean
    @Primary
    public KafkaAdmin admin(ProducerProperties producerProperties) {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        // MUST BE ONLY IF IN KRAFT BROKER ROLE IS BOTHER BROKER AND CONTROLLER
        /// configs.put(AdminClientConfig.BOOTSTRAP_CONTROLLERS_CONFIG, kafkaProperties.getBootstrapControllers());
        configs.put(AdminClientConfig.RETRIES_CONFIG, producerProperties.getRetries());
        configs.put(AdminClientConfig.RETRY_BACKOFF_MS_CONFIG, kafkaProperties.getRetryBackOffMs());
        configs.put(AdminClientConfig.RECONNECT_BACKOFF_MS_CONFIG, kafkaProperties.getBrokerReconnectBackOffMs());
        configs.put(AdminClientConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG, kafkaProperties.getBrokerReconnectBackOffMaxMs());
        //configs.put(CommonClientConfigs.CLIENT_RACK_CONFIG,kafkaProperties.getClientRack());
        configs.putAll(securityProperties.getSecurityConfig(kafkaProperties.getSecurityTypeEnum()));

        return new KafkaAdmin(configs);
    }


    @Bean(name = "kafkaTemplate")
    @Primary
    public KafkaTemplate<K, V> kafkaTemplate() {

        return new KafkaTemplate<>(producerFactory);
    }


    @Bean(name = "kafkaListenerContainerFactory")
    @Primary
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<K, V>>
    kafkaListenerContainerFactory(final ConsumerFactory<K, V> consumerFactory) {

        return getContainerListenerFactory(consumerFactory, false, consumerProperties.getAckMode(), deadLetterErrorHandler());

    }

    private KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<K, V>> getContainerListenerFactory(
            ConsumerFactory<K, V> consumerFactory, boolean batchListener,
            ContainerProperties.AckMode ackMode, DefaultErrorHandler errorHandler) {

        ConcurrentKafkaListenerContainerFactory<K, V> kafkaListenerContainerFactory = new ConcurrentKafkaListenerContainerFactory<>();
        kafkaListenerContainerFactory.getContainerProperties().setAckMode(ackMode);
        kafkaListenerContainerFactory.setConsumerFactory(consumerFactory);
        kafkaListenerContainerFactory.setBatchListener(batchListener);
        kafkaListenerContainerFactory.setConcurrency(consumerProperties.getConcurrencyLevel());
        kafkaListenerContainerFactory.setAutoStartup(consumerProperties.getAutoStart());
        kafkaListenerContainerFactory.getContainerProperties().setPollTimeout(consumerProperties.getPollTimeoutMs());
        kafkaListenerContainerFactory.setCommonErrorHandler(errorHandler);
        kafkaListenerContainerFactory.setContainerCustomizer(c ->
                c.getContainerProperties()
                        .setAuthExceptionRetryInterval(kafkaProperties.getAuthorizationExceptionRetryInterval()));
        return kafkaListenerContainerFactory;

    }

    @Bean(name = "deadLetterErrorHandler")
    public DefaultErrorHandler deadLetterErrorHandler() {

        DeadLetterPublishingRecoverer deadLetterPublishingRecoverer = createRecordRecover();

        BackOff backOff = getBackOff();

        var defaultErrorHandler = new DefaultErrorHandler(deadLetterPublishingRecoverer, backOff);
        consumerProperties.getRetryableExceptions().forEach(defaultErrorHandler::addRetryableExceptions);
        consumerProperties.getNoneRetryableExceptions().forEach(defaultErrorHandler::addNotRetryableExceptions);
        deadLetterPublishingRecoverer.removeClassification(ListenerExecutionFailedException.class);

        return defaultErrorHandler;

    }


    @Bean(name = "applicationEventPublishingErrorHandler")
    public DefaultErrorHandler applicationEventPublishingErrorHandler() {

        BackOff backOff = getBackOff();

        DefaultErrorHandler defaultErrorHandler = new DefaultErrorHandler((r,e)->createRecordRecover((ConsumerRecord<K, EventValue<K, V>>) r,e), backOff);

        defaultErrorHandler.setSeekAfterError(consumerProperties.getSeekAfterError());

        consumerProperties.getRetryableExceptions().forEach(defaultErrorHandler::addRetryableExceptions);
        consumerProperties.getNoneRetryableExceptions().forEach(defaultErrorHandler::addNotRetryableExceptions);

        return defaultErrorHandler;
    }

    @Bean
    @Primary
    public DeadLetterPublishingRecoverer createRecordRecover() {

        return new DeadLetterPublishingRecoverer(kafkaTemplate(),
                (r, e) -> {
                    log.info("Exception in the recovery: {}", e.getMessage(), e);

                    if (isRetryable.test(e)) {

                        log.info("Recoverable exception in the recovery: {}", e.getMessage(), e);
                        return new TopicPartition(r.topic()+".retry", r.partition());

                    } else {

                        log.info("Non recoverable exception in the recovery: {}", e.getMessage(), e);
                        return new TopicPartition(r.topic()+".DLT", r.partition());
                    }
                });
    }

    private void createRecordRecover(ConsumerRecord<K, EventValue<K,V>> record, Exception exception) {



        try {

            log.info("Exception in the recovery: {}", exception.getMessage(), exception);

            if (isRetryable.test(exception)) {

                log.info("Recoverable exception in the recovery: {}", exception.getMessage(), exception);

                var event = getAnonymousDefaultErrorValue(record,record.value(),exception,true);
                applicationEventPublisher.publishEvent(event);

            } else {

                log.info("Non recoverable exception in the recovery: {}", exception.getMessage(), exception);
                var event = getAnonymousDefaultErrorValue(record,record.value(),exception,false);
                applicationEventPublisher.publishEvent(event);
            }
        } catch (final Exception e) {
            log.error("Failed to save dead letter to database", e);
            var event = getAnonymousDefaultErrorValue(record,record.value(),e,false);
            applicationEventPublisher.publishEvent(event);
        }
    }

    @Primary
    @Bean
    public JsonSerializer<Object> jsonSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        JsonSerializer<Object> serializer = new JsonSerializer<>(objectMapper);
        serializer.setAddTypeInfo(false);
        return serializer;
    }

    protected EventValue<K,V> getAnonymousDefaultErrorValue(ConsumerRecord< K , EventValue<K,V>> results, EventValue<K,V> event, Throwable throwable, boolean isRetryable) {

        Throwable cause = throwable.getCause();
        String exception = cause != null ? throwable.getMessage() : throwable.getMessage();
        var causeClass= cause != null ? throwable.getClass().getName() :exception.getClass().toString();
        String stackTrace = throwable.getStackTrace()!=null&&throwable.getStackTrace().length!=0 ? Arrays.toString(throwable.getStackTrace()) :
                throwable.fillInStackTrace().toString();

        return new EventValue.EventError<K,V>(
                event.key(),
                event.value(),
                event.traceId(),
                event.topic(),
                event.eventId(),
                event.publishTime(),
                (long)results.partition(),
                results.offset(),
                exception,
                throwable.getClass().getName(),
                causeClass,stackTrace,
                isRetryable
        );
    }

    private BackOff getBackOff() {

        if (consumerProperties.getExponentialBackOff()) {

            ExponentialBackOffWithMaxRetries backOffWithMaxRetries = new ExponentialBackOffWithMaxRetries(consumerProperties.getRetryMaxAttempts());
            backOffWithMaxRetries.setInitialInterval(consumerProperties.getRetryInitialInterval());
            backOffWithMaxRetries.setMaxInterval(consumerProperties.getRetryMaxInterval());
            backOffWithMaxRetries.setMultiplier(consumerProperties.getRetryBackOffMultiplier());
            return backOffWithMaxRetries;
        }

        return new FixedBackOff(consumerProperties.getRetryFixedInterval(), consumerProperties.getRetryMaxAttempts());
    }
}
