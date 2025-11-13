package lunga.appointmentbooking;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

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
                .withCommand("start-dev --import-realm --verbose ");


    }


    @DynamicPropertySource
    public static void addProperties(DynamicPropertyRegistry registry) {

        try {

            SQLContainer.start();
            await().atMost(Duration.ofMinutes(2))
                    .until(() -> SQLContainer.isRunning() /*&& kafkaContainer1.isRunning() && kafkaContainer2.isRunning()*/);


            keycloakContainer.start();
            await().atMost(Duration.ofMinutes(2))
                    .until(() -> keycloakContainer.isRunning() /*&& kafkaContainer1.isRunning() && kafkaContainer2.isRunning()*/);

             URL = String.format("http://localhost:%s/", keycloakContainer.getMappedPort(8080));

            RESTCLIENT = RestClient.create(URL);

            registry.add("spring.datasource.url", SQLContainer::getJdbcUrl);
            registry.add("spring.datasource.username", SQLContainer::getUsername);
            registry.add("spring.datasource.password", SQLContainer::getPassword);
            registry.add("DATABASE_CATALOG", SQLContainer::getDatabaseName);
            registry.add("keycloak.urls.auth", () -> URL);
            registry.add("spring.sql.init.schema-location", () -> "classpath:db/migrations/changelog.sql");
            registry.add("spring.sql.init.mode", () -> "always");
            registry.add("default_role_types", () -> "BLOCK_USERS,BLOCKS_ADMIN");
            registry.add("default_roles.users", () -> "view_user,create_account,manage_user,manage_account,view_account,create_appointment");
            registry.add("default_roles.admin", () -> "manage_appointment,viewothers_appointment");


        } catch (Exception e) {
            log.error("Error while adding properties", e);
            throw e;
        }
    }


    @Test
    void contextLoads() {
    }

}
