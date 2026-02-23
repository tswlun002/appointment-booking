package capitec.branch.appointment.kafka.infrastructure.configuration;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import capitec.branch.appointment.kafka.infrastructure.configuration.properties.KafkaProperties;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@Slf4j
@Configuration
public class TopicConfig {

    private final KafkaProperties kafkaProperties;
    private  final  KafkaAdmin kafkaAdmin;

    public TopicConfig(KafkaProperties kafkaProperties, KafkaAdmin kafkaAdmin) {
        this.kafkaProperties = kafkaProperties;
        this.kafkaAdmin = kafkaAdmin;
        createCompactTopic();
    }

    private void createCompactTopic() {

        Map<String, Object> configurationProperties = kafkaAdmin.getConfigurationProperties();

        try {
            AdminClient client = AdminClient.create(configurationProperties);


            if (kafkaProperties.getTopicNames().isEmpty()) {
                log.warn("No topics configured");
                throw new RuntimeException("Internal Server Error");
            }
            log.info("Creating topics ..");
            Set<String> topics = kafkaProperties.getTopicNames();
            Set<NewTopic> collect = topics.stream()
                    .map(t -> TopicBuilder
                            .name(t)
                            .partitions(kafkaProperties.getPartitions())
                            .replicas(kafkaProperties.getReplicas())
                            .build()
                    )
                    .collect(Collectors.toSet());

            CreateTopicsResult topicCreated = client.createTopics(collect);

            logCreatedTopics(topics, topicCreated);
        } catch (Exception e) {

            log.warn("Error while creating topics", e);
        }

    }

    private  void logCreatedTopics(final Set<String> topicNames, CreateTopicsResult result){
        topicNames.forEach(name->{
            result.config(name).whenComplete((config,error)->{

                if(error==null){

                    log.info("✅ Topic:{} created successfully",name);
                }else {

                    log.warn("❌ Topic:{} not created ‼️, error:{}",name, error.getMessage(), error);
                }

            });
        });
    }
}
