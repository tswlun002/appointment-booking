package capitec.branch.appointment.kafka.infrastructure.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.ProducerFactory;
import capitec.branch.appointment.kafka.infrastructure.configuration.properties.KafkaProperties;
import capitec.branch.appointment.kafka.infrastructure.configuration.properties.ProducerProperties;
import capitec.branch.appointment.kafka.infrastructure.configuration.properties.SecurityProperties;
import capitec.branch.appointment.kafka.domain.*;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@EnableKafka
@RequiredArgsConstructor
public class KafkaProducerConfig<K extends Serializable, V extends Serializable> {

    private final KafkaProperties kafkaProperties;
    private final ProducerProperties producerProperties;

    public Map<String, Object> producerConfig() {

        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        props.put(ProducerConfig.RECONNECT_BACKOFF_MS_CONFIG, kafkaProperties.getBrokerReconnectBackOffMs());
        props.put(ProducerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG, kafkaProperties.getBrokerReconnectBackOffMaxMs());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, producerProperties.getKeySerializerClass());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, producerProperties.getValueSerializerClass());
        props.put(ProducerConfig.ACKS_CONFIG, producerProperties.getAcks());
        props.put(ProducerConfig.LINGER_MS_CONFIG, producerProperties.getLingerMs());
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, producerProperties.getCompressionType());
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, producerProperties.getRequestTimeoutMs());
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, producerProperties.getIdempotent());
        props.put(ProducerConfig.RETRIES_CONFIG, producerProperties.getRetries());
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, producerProperties.getMaxInFlightRequestsPerConnection());
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, producerProperties.getDeliverTimeOutMs());
        props.putAll(producerProperties.getAdditionalProperties());

        return props;
    }

    @Bean
    @Primary
    public ProducerFactory<K, V> producerFactory(final SecurityProperties securityProperties) {
        var configs = new HashMap<>(securityProperties.getSecurityConfig(kafkaProperties.getSecurityTypeEnum()));
        configs.putAll(producerConfig());

        return new DefaultKafkaProducerFactory<>(configs);

    }
}
