package capitec.branch.appointment.kafka.infrastructure.configuration;

import capitec.branch.appointment.sharekernel.EventToJSONMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;


@Slf4j
@EnableKafka
@Configuration
@RequiredArgsConstructor
public class KafkaConsumerConfig<K extends  Serializable,V extends Serializable> {

    private final KafkaProperties kafkaProperties;
    private final ConsumerProperties consumerProperties;

    public Map<String, Object> consumerConfigs() {

        Map<String, Object> props = new HashMap<>();

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        props.put(ConsumerConfig.RECONNECT_BACKOFF_MS_CONFIG, kafkaProperties.getBrokerReconnectBackOffMs());
        props.put(ConsumerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG, kafkaProperties.getBrokerReconnectBackOffMaxMs());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerProperties.getGroupId());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, consumerProperties.getAutoOffsetReset());
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, consumerProperties.getSessionTimeoutMs());
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, consumerProperties.getHeartbeatIntervalMs());
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, consumerProperties.getMaxPollIntervalMs());
        props.put(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, consumerProperties.getAutoCreateTopics());
        props.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, consumerProperties.getMaxPartitionFetchBytesDefault() *
                consumerProperties.getMaxPartitionFetchRecordsBoostFactor());
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, consumerProperties.getMaxPollRecords());
        props.put(ConsumerConfig.RETRY_BACKOFF_MS_CONFIG, consumerProperties.getRetryBackOffMs());
        props.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, consumerProperties.getRequestTimeoutSkip());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, consumerProperties.getAutoCommit());
        props.putAll(consumerProperties.getAdditionalProperties());

        return props;
    }

    @Bean
    @Primary
    public ConsumerFactory<K, V> consumerFactory(final SecurityProperties securityProperties) {
        Map<String, Object> props = new HashMap<>(securityProperties.getSecurityConfig(kafkaProperties.getSecurityTypeEnum()));
        props.putAll(consumerConfigs());

        // Use shared ObjectMapper that respects @JsonTypeInfo annotations on EventValue and MetaData
        JsonDeserializer<V> valueDeserializer = new JsonDeserializer<>(EventToJSONMapper.getMapper());

        // Trust packages for polymorphic deserialization
        String[] trustedPackages = consumerProperties.getAllowedPackages().split(",");
        valueDeserializer.addTrustedPackages(trustedPackages);

        // Do NOT use type headers - rely on @JsonTypeInfo embedded in JSON payload
        valueDeserializer.setUseTypeHeaders(false);

        // Wrap with ErrorHandlingDeserializer if bypass is enabled
        Deserializer<K> kDeserializer = (Deserializer<K>) new StringDeserializer();
        if (consumerProperties.getBypassUnserializedMessages()) {
            ErrorHandlingDeserializer<V> errorHandlingDeserializer = new ErrorHandlingDeserializer<>(valueDeserializer);
            return new DefaultKafkaConsumerFactory<>(props, kDeserializer, errorHandlingDeserializer);
        }

        return new DefaultKafkaConsumerFactory<>(props, kDeserializer, valueDeserializer);
    }
}
