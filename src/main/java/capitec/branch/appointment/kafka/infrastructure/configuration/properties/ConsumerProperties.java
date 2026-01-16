package capitec.branch.appointment.kafka.infrastructure.configuration.properties;


import capitec.branch.appointment.exeption.MailSenderException;
import lombok.Getter;
import lombok.Setter;
import org.apache.kafka.common.errors.NotEnoughReplicasException;
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.security.oauthbearer.internals.secured.ValidateException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionTimedOutException;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ConfigurationProperties(prefix = "kafka.consumer-config")
@Component
@Getter
@Setter
public class ConsumerProperties {
    private String keyDeserializerClass ="org.apache.kafka.common.serialization.StringDeserializer";
    private String valueDeserializerClass="org.springframework.kafka.support.serializer.JsonDeserializer";
    private Boolean bypassUnserializedMessages =true;
    private String groupId="default";
    private String autoOffsetReset="latest";
    private Boolean batchListener=false;
    private Boolean autoStart=true;
    private Boolean autoCommit=true;
    private Boolean autoCreateTopics=false;
    private Integer concurrencyLevel=1;
    private Integer sessionTimeoutMs=10000;
    private Integer  heartbeatIntervalMs=3000;
    private Integer maxPollIntervalMs=30000;
    private Integer maxPollRecords=50;
    private Integer maxPartitionFetchBytesDefault=200000000;
    private Integer maxPartitionFetchRecordsBoostFactor=1;
    private Long pollTimeoutMs=150L;
    private Map<String,String> additionalProperties = new HashMap<>();
    private Boolean exponentialBackOff=true;
    private Integer retryBackOffMs=1000;
    private Integer retryBackOffMultiplier=2;
    private Integer retryFixedInterval=2000;
    private Integer retryInitialInterval=1000;
    private Integer retryMaxInterval=2000;
    private Integer retryMaxAttempts=1000;
    private Integer requestTimeoutSkip=5000;
    private String allowedPackages="capitec.branch.appointment.kafka";
    private List<String> allowedPackagesSubdirectories=List.of("model");
    private Long initialRetryInterval = 1000L;


    private Set<Class<? extends Exception>> retryableExceptions=Set.of(
            SocketException.class, TransactionTimedOutException.class,
            ConnectException.class, UnknownHostException.class, IOException.class,
            TimeoutException.class, NotEnoughReplicasException.class, DeserializationException.class,
            MailSenderException.class

    );

    private List<Class<? extends Exception>> noneRetryableExceptions=List.of(
          NullPointerException.class, ValidateException.class
    );

    private Boolean seekAfterError=false;
    private ContainerProperties.AckMode ackMode =ContainerProperties.AckMode.RECORD;
    private ContainerProperties.AckMode batchAckMode=ContainerProperties.AckMode.BATCH;

}
