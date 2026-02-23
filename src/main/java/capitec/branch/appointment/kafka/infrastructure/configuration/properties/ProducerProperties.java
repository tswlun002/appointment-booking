package capitec.branch.appointment.kafka.infrastructure.configuration.properties;


import lombok.Getter;
import lombok.Setter;
import org.apache.kafka.common.errors.NotEnoughReplicasAfterAppendException;
import org.apache.kafka.common.errors.NotEnoughReplicasException;
import org.apache.kafka.common.errors.TimeoutException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionTimedOutException;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


@ConfigurationProperties(prefix = "kafka.producer-config")
@Component
@Getter
@Setter
public class ProducerProperties {

    private String keySerializerClass ="org.apache.kafka.common.serialization.StringSerializer";
    private String valueSerializerClass="org.springframework.kafka.support.serializer.JsonSerializer";
    private String acks="all";
    private String retries="5";
    private String lingerMs="5";
    private Integer requestTimeoutMs=60000;
    private Boolean idempotent=true;
    private String compressionType="snappy";
    private Integer maxInFlightRequestsPerConnection=5;
    private Integer deliverTimeOutMs=360000;
    private Map<String, String> additionalProperties = new HashMap<String, String>();
    private Set<Class<? extends Exception>> retryableExceptions=Set.of(
            SocketException.class, TransactionTimedOutException.class,
            ConnectException.class, UnknownHostException.class,
            IOException.class, TimeoutException.class,
            NotEnoughReplicasException.class,
            NotEnoughReplicasAfterAppendException.class

    );

}
