package capitec.branch.appointment;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.*;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import static org.awaitility.Awaitility.await;

@Slf4j
@SpringBootTest(properties = "spring.profiles.active=test", webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class AppointmentBookingApplicationTests {


    public final static String SQL_NETWORK_ALIAS = "users_db_test";
    public final static String PASSWORD = "users";
    public final static String USERNAME = "admin";
    protected static Network NETWORK = Network.newNetwork();

    protected static PostgreSQLContainer<?> SQLContainer =  new PostgreSQLContainer<>("postgres:17")

            .withPassword("123456users")
            .withUsername("root")
            .withDatabaseName("users_db")
            .withUrlParam("loggerLevel", "TRACE")
            .withExposedPorts(5432)
            .withNetworkAliases(SQL_NETWORK_ALIAS)
            .withNetwork(NETWORK);

    protected static GenericContainer<?> keycloakContainer;

    protected static RestClient RESTCLIENT;
    protected static String URL;

    static {

        HashMap<String, String> keycloakEnv = new HashMap<>(Map.of("KC_BOOTSTRAP_ADMIN_USERNAME", USERNAME,
                "KC_BOOTSTRAP_ADMIN_PASSWORD", PASSWORD, "KEYCLOAK_JDBC_PARAMS", "'sslmode=require'",
                "DB_VENDOR", "postgres",
                "DB_ADDR", SQL_NETWORK_ALIAS,
                "DB_URL", "jdbc:postgresql://" + SQL_NETWORK_ALIAS + "/" + SQLContainer.getDatabaseName(),
                "DB_SCHEMA", SQLContainer.getDatabaseName(),
                "DB_PASSWORD", SQLContainer.getPassword(), "DB_USERNAME", SQLContainer.getUsername(),
                "KC_HEALTH_ENABLED", "true")
        );

        keycloakEnv.put("KEYCLOAK_IMPORT", "/opt/keycloak/data/import/realm.json");
        keycloakEnv.put("JAVA_OPTS", "-Dkeycloak.profile.feature.token_exchange=enabled -Dkeycloak.profile.feature.admin_fine_grained_authz=enabled");
        keycloakEnv.put("KC_FEATURES", "token-exchange");
        Path jarPath = Paths.get(System.getProperty("user.dir"))
                .getParent()
                .resolve("appointment-booking/validate-credential-module/build/libs/validate-credential-module-APPOINTMENT-BOOKING-UNSET-VERSION.jar");

        log.info("SPI JAR PATH: {}", jarPath);
        keycloakContainer = new GenericContainer<>("quay.io/keycloak/keycloak:26.1.2")
                .withEnv(keycloakEnv)
                .dependsOn(SQLContainer)
                .withNetwork(NETWORK)
                .withCreateContainerCmdModifier(cmd ->
                        cmd.withHostName("keycloak_test")
                                .withAliases("keycloak_test")
                                .withName("keycloak_test")
                                .withNetworkDisabled(false)
                )
                .withExposedPorts(8080)
                .withNetworkAliases("keycloak_test")
                .withClasspathResourceMapping("realm.json", "/opt/keycloak/data/import/realm.json", BindMode.READ_WRITE)
                .withCopyFileToContainer(MountableFile.forHostPath(jarPath), "/opt/keycloak/providers/validate-credential-module-APPOINTMENT-BOOKING-UNSET-VERSION.jar")
                .withCommand("start-dev --import-realm --verbose ");


    }
    protected static KafkaContainer kafkaContainer = getKafkaControllerContainer(1);
    //protected static KafkaContainer kafkaContainer1 = getKafkaControllerContainer(2);



    private  static KafkaContainer getKafkaControllerContainer(int nodeId){
        var  BROKER_SERVICE_NAME = "kafka-controller-test"+nodeId;
        return new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.7.6"))
                .withNetwork(NETWORK)
                .withNetworkAliases(BROKER_SERVICE_NAME)
                .withEnv("KAFKA_NODE_ID", String.valueOf(nodeId))
                .withEnv("KAFKA_BROKER_ID", String.valueOf(nodeId))
                .withEnv("KAFKA_PROCESS_ROLES", "controller")
                //.withEnv("KAFKA_LISTENERS", "CONTROLLER://:9094")
                .withEnv("KAFKA_ADVERTISED_LISTENERS", (String) null)
                .withEnv("KAFKA_CONTROLLER_LISTENER_NAMES", "CONTROLLER")
                .withEnv("KAFKA_CONTROLLER_QUORUM_VOTERS", nodeId +"@"+BROKER_SERVICE_NAME+":9094")
                .withEnv("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP", "CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,BROKER:PLAINTEXT")
                .withEnv("KAFKA_CLUSTER_ID", "q1Sh-9_ISia_zwGINzRvyQ")
                .withEnv("KAFKA_INTER_BROKER_LISTENER_NAME", "BROKER")
                .withEnv("CLUSTER_ID", "q1Sh-9_ISia_zwGINzRvyQ")
                .withEnv("KAFKA_LOG_DIRS", "/tmp/kraft-combined-logs")
                .withEnv("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR","2")
                .withEnv("KAFKA_MIN_INSYNC_REPLICAS", "2")
                .withEnv("KAFKA_DEFAULT_REPLICATION_FACTOR", "2")
                .withExposedPorts(9093,9094)
                .withEnv(
                        "KAFKA_OPTS",
                        "-Djava.net.preferIPv4Stack=true " +
                                "-Dsun.net.inetaddr.ttl=0 " +
                                "-Dnetworkaddress.cache.ttl=0 " +
                                "-Dnetworkaddress.cache.negative.ttl=0"
                )
                .withCreateContainerCmdModifier(cmd ->
                        cmd.withHostName(BROKER_SERVICE_NAME)
                                .withAliases(BROKER_SERVICE_NAME)
                                .withName(BROKER_SERVICE_NAME)

                )
                .withKraft()
                .waitingFor(Wait.forLogMessage(".*Kafka Server started.*", 1))
                .withStartupTimeout(Duration.ofMinutes(1));
    }


    @DynamicPropertySource
    public static void addProperties(DynamicPropertyRegistry registry) {

        try {

            // DATABASE
            SQLContainer.start();await().atMost(Duration.ofMinutes(2)).until(() -> SQLContainer.isRunning() /*&& kafkaContainer1.isRunning() && kafkaContainer2.isRunning()*/);
            registry.add("spring.datasource.url", SQLContainer::getJdbcUrl);
            registry.add("spring.datasource.username", SQLContainer::getUsername);
            registry.add("spring.datasource.password", SQLContainer::getPassword);
            registry.add("DATABASE_CATALOG", SQLContainer::getDatabaseName);
            registry.add("spring.sql.init.schema-location", () -> "classpath:db/migrations/changelog.sql");
            registry.add("spring.sql.init.mode", () -> "always");

            //KEYCLOAK
            keycloakContainer.start();
            await().atMost(Duration.ofMinutes(1)).until(() -> keycloakContainer.isRunning() /*&& kafkaContainer1.isRunning() && kafkaContainer2.isRunning()*/);
            URL = String.format("http://localhost:%s/", keycloakContainer.getMappedPort(8080));
            RESTCLIENT = RestClient.create(URL);
            registry.add("keycloak.urls.auth", () -> URL);
            registry.add("default_role_types", () -> "APP_USERS,APP_ADMINS");
            registry.add("default_roles.users", () -> "app_user");
            registry.add("default_roles.admin", () -> "app_admin");

            // KAFKA
            kafkaContainer.start();
            await().atMost(Duration.ofMinutes(2)).until(() -> kafkaContainer.isRunning()/* && kafkaContainer1.isRunning() && kafkaContainer2.isRunning()*/);
            String bootstrapServers = kafkaContainer.getBootstrapServers() ;//+ ", " + kafkaContainer1.getBootstrapServers()+ ", " + kafkaContainer2.getBootstrapServers() ;
            registry.add("kafka.common-config.bootstrap-servers", () -> bootstrapServers);
            registry.add("kafka.common-config.bootstrapServers", () -> bootstrapServers);
            registry.add("kafka.common-config.bootstrapServers", () -> bootstrapServers);
            registry.add("spring.kafka.admin.properties.bootstrap-servers", () -> bootstrapServers);
            registry.add("spring.kafka.bootstrap-servers", () -> bootstrapServers);
            registry.add("spring.kafka.producer.bootstrap-servers", () -> bootstrapServers);
            registry.add("spring.kafka.consumer.bootstrap-servers", () -> bootstrapServers);
            registry.add("spring.kafka.streams.bootstrap-servers", () -> bootstrapServers);
            registry.add("kafka.producer-config.linger-ms", () -> 0);
            registry.add("kafka.error.persistence.enabled", () -> "false");
            registry.add("kafka.common-config.topic-names", () -> "email-verified-event,registration-event,password-reset-request-event,deleted-user-account-event,deleted-user-account-request-event,complete-registration-event");
            registry.add("kafka.common-config.replicas", () -> 1);

            //Security
            var issuer = URL+"realms/AppointmentSystemTest";
            registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", () -> issuer+"/protocol/openid-connect/certs");
            registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> issuer);
            registry.add("allowed_origins.urls",()->"*");
            registry.add("allowed_origins.cache_period",()->"30");


        } catch (Exception e) {
            log.error("Error while adding properties", e);
            throw e;
        }
    }


    @Test
    void contextLoads() {
    }

}
