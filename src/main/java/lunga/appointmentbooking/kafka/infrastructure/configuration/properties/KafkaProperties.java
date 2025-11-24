package lunga.appointmentbooking.kafka.infrastructure.configuration.properties;


import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lunga.appointmentbooking.kafka.domain.DEAD_LETTER_TOPIC;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@ConfigurationProperties(prefix = "kafka.common-config")
@Component
@Getter
@Setter
public class KafkaProperties {
   private String bootstrapServers = "Broker1:29092,Broker2:29093,Broker3:29094";
   private String bootstrapControllers= "Broker1:29092,Broker2:29093,Broker3:29094";
   private Duration  authorizationExceptionRetryInterval = Duration.ofSeconds(50);
   private Set<String> topicNames = new HashSet<>(Set.of(DEAD_LETTER_TOPIC.DEAD.getTopic(), DEAD_LETTER_TOPIC.RETRY.getTopic()));
   private Integer partitions = 3;
   private Integer replicas = 3;
   private  Map<String,String> topicConfig = new HashMap<>();
   private  Map<String,String> compatTopicConfig = new HashMap<>();
   private Integer retryBackOffMs= 800;
   private Integer brokerReconnectBackOffMs = 1000;
   private Integer brokerReconnectBackOffMaxMs = 30000;
   private String  prefix ="save-dev";
   private  Boolean topicCreator = false;
   //private String clientRack="EXTERNAL";
   @Value("#{T(lunga.appointmentbooking.kafka.infrastructure.configuration.properties.SECURITY_TYPE_ENUM).valueOf(${security.type:'NONE'})}")
   private SECURITY_TYPE_ENUM securityTypeEnum;


}
