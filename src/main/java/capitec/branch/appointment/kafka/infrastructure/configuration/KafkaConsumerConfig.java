package capitec.branch.appointment.kafka.infrastructure.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import capitec.branch.appointment.kafka.infrastructure.configuration.properties.ConsumerProperties;
import capitec.branch.appointment.kafka.infrastructure.configuration.properties.KafkaProperties;
import capitec.branch.appointment.kafka.infrastructure.configuration.properties.SecurityProperties;
import capitec.branch.appointment.kafka.domain.EventValue;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;


@Slf4j
@EnableKafka
@Configuration
@RequiredArgsConstructor
public class KafkaConsumerConfig<K extends Serializable,V extends EventValue> {

    private static final String SPRING_JSON_PACKAGE = "spring.json.trusted.packages";
    private final KafkaProperties kafkaProperties;
    private final ConsumerProperties consumerProperties;

    public Map<String, Object> consumerConfigs() {

        Map<String, Object> props = new HashMap<>();

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        props.put(ConsumerConfig.RECONNECT_BACKOFF_MS_CONFIG, kafkaProperties.getBrokerReconnectBackOffMs());
        props.put(ConsumerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG,kafkaProperties.getBrokerReconnectBackOffMaxMs());
        if(consumerProperties.getBypassUnserializedMessages()){

            props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class.getName());
            props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class.getName());
            props.put("spring.deserializer.key.delegate.class",consumerProperties.getKeyDeserializerClass());
            props.put("spring.deserializer.value.delegate.class",consumerProperties.getValueDeserializerClass());

        }
        else {
            props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, consumerProperties.getKeyDeserializerClass());
            props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, consumerProperties.getValueDeserializerClass());
        }
        props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerProperties.getGroupId());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, consumerProperties.getAutoOffsetReset());
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, consumerProperties.getSessionTimeoutMs());
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, consumerProperties.getHeartbeatIntervalMs());
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, consumerProperties.getMaxPollIntervalMs());
        props.put(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, consumerProperties.getAutoCreateTopics());
        props.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, consumerProperties.getMaxPartitionFetchBytesDefault()*
                consumerProperties.getMaxPartitionFetchRecordsBoostFactor());
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, consumerProperties.getMaxPollRecords());
        props.put(ConsumerConfig.RETRY_BACKOFF_MS_CONFIG, consumerProperties.getRetryBackOffMs());
        props.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG,consumerProperties.getRequestTimeoutSkip());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, consumerProperties.getAutoCommit());
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, EventValue.class.getName());
        var allowedPackages =consumerProperties.getAllowedPackages()+"."+String.join(","+consumerProperties.getAllowedPackages()+".", consumerProperties.getAllowedPackagesSubdirectories());
        props.put(SPRING_JSON_PACKAGE,allowedPackages);
        props.putAll(consumerProperties.getAdditionalProperties());

        return props;

    }

    @Bean
    @Primary
    public ConsumerFactory<K, V> consumerFactory(final SecurityProperties securityProperties) {
        Map<String, Object> props = new HashMap<>(securityProperties.getSecurityConfig(kafkaProperties.getSecurityTypeEnum()));
        props.putAll(consumerConfigs());
        return new DefaultKafkaConsumerFactory<>(props);

    }

}   

