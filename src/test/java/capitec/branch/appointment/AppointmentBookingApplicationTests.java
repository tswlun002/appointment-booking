package capitec.branch.appointment;

import capitec.branch.appointment.user.domain.User;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
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

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

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
            /*.withCreateContainerCmdModifier(cmd -> cmd
                  //  .withMemory(384 * 1024 * 1024L)
            )*/
            .withNetwork(NETWORK)
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)));

    protected static GenericContainer<?> keycloakContainer;

    protected static RestClient RESTCLIENT;
    protected static String URL;



    static {

        HashMap<String, String> keycloakEnv = new HashMap<>(Map.of(
                "KC_BOOTSTRAP_ADMIN_USERNAME", USERNAME,
                "KC_BOOTSTRAP_ADMIN_PASSWORD", PASSWORD,
                "KC_DB", "postgres",
                "KC_DB_URL", "jdbc:postgresql://" + SQL_NETWORK_ALIAS + ":5432/" + SQLContainer.getDatabaseName(),
                "KC_DB_USERNAME", SQLContainer.getUsername(),
                "KC_DB_PASSWORD", SQLContainer.getPassword(),
                "KC_LOG_LEVEL", "INFO")
        );

        keycloakEnv.put("KEYCLOAK_IMPORT", "/opt/keycloak/data/import/realm.json");
        keycloakEnv.put("KC_FEATURES", "token-exchange");
        keycloakEnv.put("KC_HEALTH_ENABLED", "true");
        keycloakEnv.put("KC_METRICS_ENABLED", "true");




        Path valiadteCredJarPath = Paths.get(System.getProperty("user.dir"))
                .getParent()
                .resolve("appointment-booking/validate-credential-module/build/libs/validate-credential-module-APPOINTMENT-BOOKING-UNSET-VERSION.jar");


        Path jarPathUsernameGenerator = Paths.get(System.getProperty("user.dir"))
                .getParent()
                .resolve("appointment-booking/generate-username-ui-register-module/build/libs/generate-username-module-APPOINTMENT-BOOKING-UNSET-VERSION.jar");

        log.info("SPI JAR PATH: {}", valiadteCredJarPath);
        log.info("SPI JAR PATH: {}", jarPathUsernameGenerator);

        keycloakContainer = new GenericContainer<>("quay.io/keycloak/keycloak:26.0.4")
                .withEnv(keycloakEnv)
                .dependsOn(SQLContainer)
                .withNetwork(NETWORK)
                .withCreateContainerCmdModifier(cmd ->
                        cmd.withHostName("keycloak_test")
                                .withAliases("keycloak_test")
                                .withName("keycloak_test")
                                .withNetworkDisabled(false)
                                .withMemory(1024 * 1024 * 1024L)
                )
                .withExposedPorts(8080, 9000)
                .withNetworkAliases("keycloak_test")
                .withClasspathResourceMapping("realm.json", "/opt/keycloak/data/import/realm.json", BindMode.READ_WRITE)
                // SPI JARs temporarily disabled for testing
                //.withCopyFileToContainer(MountableFile.forHostPath(valiadteCredJarPath), "/opt/keycloak/providers/validate-credential-module-APPOINTMENT-BOOKING-UNSET-VERSION.jar")
                //.withCopyFileToContainer(MountableFile.forHostPath(jarPathUsernameGenerator), "/opt/keycloak/providers/generate-username-module-APPOINTMENT-BOOKING-UNSET-VERSION.jar")
                .withCommand("start-dev", "--import-realm", "--verbose")
                .withLogConsumer(outputFrame -> log.info("KEYCLOAK: {}", outputFrame.getUtf8String()))
                .waitingFor(Wait.forLogMessage(".*Keycloak.*started.*", 1).withStartupTimeout(Duration.ofMinutes(5)));


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
                                //.withMemory(512 * 1024 * 1024L)

                )
                .withKraft()
                .waitingFor(Wait.forLogMessage(".*Kafka Server started.*", 1))
                .withStartupTimeout(Duration.ofMinutes(1));
    }

    public  static GenericContainer<?> wiremockContainer = new GenericContainer<>(
            DockerImageName.parse("wiremock/wiremock:latest"))
            /*.withCreateContainerCmdModifier(cmd -> cmd
                    .withMemory(256 * 1024 * 1024L))*/
            .withExposedPorts(8080); // WireMock's default port



    @DynamicPropertySource
    public static void addProperties(DynamicPropertyRegistry registry) {

        try {

            // DATABASE
            SQLContainer.start();await().atMost(Duration.ofMinutes(2)).until(() -> SQLContainer.isRunning() /*&& kafkaContainer1.isRunning() && kafkaContainer2.isRunning()*/);
            registry.add("spring.datasource.url", SQLContainer::getJdbcUrl);
            registry.add("spring.datasource.username", SQLContainer::getUsername);
            registry.add("spring.datasource.password", SQLContainer::getPassword);
            registry.add("DATABASE_CATALOG", SQLContainer::getDatabaseName);
            registry.add("spring.sql.init.schema-location", () -> "classpath:db/migrations/changelog-master.xml");
            registry.add("spring.sql.init.mode", () -> "always");

            //KEYCLOAK
            keycloakContainer.start();
            await().atMost(Duration.ofMinutes(2)).until(() -> keycloakContainer.isRunning() /*&& kafkaContainer1.isRunning() && kafkaContainer2.isRunning()*/);
            URL = String.format("http://localhost:%s/", keycloakContainer.getMappedPort(8080));
            RESTCLIENT = RestClient.create(URL);
            registry.add("keycloak.urls.auth", () -> URL);
            registry.add("default_role_types", () -> "APP_USERS,APP_ADMINS");
            registry.add("default_roles.users", () -> "app_user,app_client,app_guest");
            registry.add("default_roles.admin", () -> "app_admin");

            // Keycloak configuration from realm.json
            registry.add("KEYCLOAK_REALM", () -> "appointment-booking-DEV");
            registry.add("KEYCLOAK_DOMAIN", () -> URL);
            registry.add("KEYCLOAK_ADMIN_CLIENT_ID", () -> "appointment-booking");
            registry.add("KEYCLOAK_ADMIN_CLIENT_SECRETE", () -> "37B9EoUIVcDGxJexxDBuVismiDy53uMd");
            registry.add("KEYCLOAK_ADMIN_SCOPE", () -> "openid");
            registry.add("KEYCLOAK_ADMIN_GRANT_TYPE", () -> "client_credentials");
            registry.add("KEYCLOAK_CLIENT_TYPE", () -> "confidential");
            registry.add("KEYCLOAK_AUTH_URL", () -> URL);
            registry.add("KEYCLOAK_USER_AUTH_TYPE", () -> "password");
            registry.add("KEYCLOAK_PASSWORD", () -> PASSWORD);
            registry.add("KEYCLOAK_USERNAME", () -> USERNAME);
            registry.add("DEFAULT_USERS_ROLES", () -> "app_user,app_client,app_guest");
            registry.add("DEFAULT_ADMIN_ROLES", () -> "app_admin");

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
            registry.add("kafka.common-config.topic-names", () -> "email-verified-event,registration-event,password-reset-request-event,deleted-user-account-event,deleted-user-account-request-event,complete-registration-event," +
                    "appointment-rescheduled,appointment-booked,appointment-canceled,attended-appointment,password-updated-event");
            registry.add("kafka.common-config.replicas", () -> 1);
            registry.add("kafka.listen.consumer.auto.start", () -> false);
            registry.add("kafka.listen.produce.auto.start", () -> false);

            //Security
            var issuer = URL+"realms/appointment-booking-DEV";
            registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", () -> issuer+"/protocol/openid-connect/certs");
            registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> issuer);
            registry.add("AUTH_SERVER_TOKEN_URL", () -> issuer+"/protocol/openid-connect/certs");
            registry.add("ISSUER_URI", () -> issuer);
            registry.add("allowed_origins.urls",()->"*");
            registry.add("allowed_origins.cache_period",()->"30");

            // Mail configuration (mock values for testing)
            registry.add("MAIL_HOST", () -> "smtp.test.local");
            registry.add("MAIL_USERNAME", () -> "test@test.local");
            registry.add("MAIL_PASSWORD", () -> "testpassword123");
            registry.add("mail.host", () -> "smtp.test.local");
            registry.add("mail.username", () -> "test@test.local");
            registry.add("mail.password", () -> "testpassword123");

            // Short OTP expiration for faster tests
            registry.add("otp.expire.datetime", () -> 5);
            registry.add("otp.expire.chron-units", () -> "SECONDS");
            // Short cooldown for faster rate limit testing (1 second)
            registry.add("rate-limit.otp-resend.cooldown-seconds", () -> 1);
            // Keep max attempts at 5 for realistic testing
            registry.add("rate-limit.otp-resend.max-attempts", () -> 3);
            // Short window for faster cleanup
            registry.add("rate-limit.otp-resend.window-minutes", () -> 3);

            //Client domain
            wiremockContainer.start();
            await().atMost(Duration.ofMinutes(2)).until(() -> wiremockContainer.isRunning());
            registry.add("client-domain.baseurl", () ->
                    String.format("http://%s:%d",
                            wiremockContainer.getHost(),
                            wiremockContainer.getFirstMappedPort()));

            // Mock Nager server
            //wiremockNagerServer.start();
            await().atMost(Duration.ofMinutes(2)).until(() -> wiremockContainer.isRunning());
            registry.add("holidays-client.baseurl", () ->
                    String.format("http://%s:%d",
                            wiremockContainer.getHost(),
                            wiremockContainer.getFirstMappedPort()));

            // Capitec Branch Locator API (uses same WireMock server as client-domain)
            //wiremockClientDomainServer.start();
            await().atMost(Duration.ofMinutes(2)).until(() -> wiremockContainer.isRunning());
            registry.add("capitec.branch-locator-api.url", () ->
                    String.format("http://%s:%d/branches",
                            wiremockContainer.getHost(),
                            wiremockContainer.getFirstMappedPort()));



        } catch (Exception e) {
            log.error("Error while adding branchConfigs", e);
            throw e;
        }
    }

    public static   void wireMockGetHolidayByYearAndCountryCode(User user, String idNumber) {
        var  wireMock = new WireMock(wiremockContainer.getHost(), wiremockContainer.getFirstMappedPort());

        // 1. Define the mock response (the stub)

        String expectedBody = String.format("""
                        {
                          "username": "%s",
                          "firstname": "%s",
                          "lastname": "%s",
                          "email": "%s",
                          "enabled": "%s"
                        }
                        """, user.getUsername(), user.getFirstname()
                , user.getLastname(), user.getEmail(), true);

        wireMock.register(WireMock.get(WireMock.urlPathEqualTo("/v1/clients"))
                .withQueryParam("IDNumber",WireMock.equalTo(idNumber))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(expectedBody)));
    }

    public static   void wireMockGetHolidayByYearAndCountryCode(String year, String countryCode) {

        String uri = String.format("/api/v3/PublicHolidays/%s/%s", year, countryCode);
        var  wireMock = new WireMock(wiremockContainer.getHost(), wiremockContainer.getFirstMappedPort());
        String expectedBody = """
                        [
                            {
                                "date": "2025-01-01",
                                "localName": "New Year's Day",
                                "name": "New Year's Day",
                                "countryCode": "ZA",
                                "fixed": false,
                                "global": true,
                                "counties": null,
                                "launchYear": null,
                                "types": [
                                    "Public"
                                ]
                            },
                            {
                                "date": "2025-03-21",
                                "localName": "Human Rights Day",
                                "name": "Human Rights Day",
                                "countryCode": "ZA",
                                "fixed": false,
                                "global": true,
                                "counties": null,
                                "launchYear": null,
                                "types": [
                                    "Public"
                                ]
                            },
                            {
                                "date": "2025-04-18",
                                "localName": "Good Friday",
                                "name": "Good Friday",
                                "countryCode": "ZA",
                                "fixed": false,
                                "global": true,
                                "counties": null,
                                "launchYear": null,
                                "types": [
                                    "Public"
                                ]
                            },
                            {
                                "date": "2025-04-21",
                                "localName": "Family Day",
                                "name": "Family Day",
                                "countryCode": "ZA",
                                "fixed": false,
                                "global": true,
                                "counties": null,
                                "launchYear": null,
                                "types": [
                                    "Public"
                                ]
                            },
                            {
                                "date": "2025-04-28",
                                "localName": "Freedom Day",
                                "name": "Freedom Day",
                                "countryCode": "ZA",
                                "fixed": false,
                                "global": true,
                                "counties": null,
                                "launchYear": null,
                                "types": [
                                    "Public"
                                ]
                            },
                            {
                                "date": "2025-05-01",
                                "localName": "Workers' Day",
                                "name": "Workers' Day",
                                "countryCode": "ZA",
                                "fixed": false,
                                "global": true,
                                "counties": null,
                                "launchYear": null,
                                "types": [
                                    "Public"
                                ]
                            },
                            {
                                "date": "2025-06-16",
                                "localName": "Youth Day",
                                "name": "Youth Day",
                                "countryCode": "ZA",
                                "fixed": false,
                                "global": true,
                                "counties": null,
                                "launchYear": null,
                                "types": [
                                    "Public"
                                ]
                            },
                            {
                                "date": "2025-08-09",
                                "localName": "National Women's Day",
                                "name": "National Women's Day",
                                "countryCode": "ZA",
                                "fixed": false,
                                "global": true,
                                "counties": null,
                                "launchYear": null,
                                "types": [
                                    "Public"
                                ]
                            },
                            {
                                "date": "2025-09-24",
                                "localName": "Heritage Day",
                                "name": "Heritage Day",
                                "countryCode": "ZA",
                                "fixed": false,
                                "global": true,
                                "counties": null,
                                "launchYear": null,
                                "types": [
                                    "Public"
                                ]
                            },
                            {
                                "date": "2025-12-16",
                                "localName": "Day of Reconciliation",
                                "name": "Day of Reconciliation",
                                "countryCode": "ZA",
                                "fixed": false,
                                "global": true,
                                "counties": null,
                                "launchYear": null,
                                "types": [
                                    "Public"
                                ]
                            },
                            {
                                "date": "2025-12-25",
                                "localName": "Christmas Day",
                                "name": "Christmas Day",
                                "countryCode": "ZA",
                                "fixed": false,
                                "global": true,
                                "counties": null,
                                "launchYear": null,
                                "types": [
                                    "Public"
                                ]
                            },
                            {
                                "date": "2025-12-26",
                                "localName": "St. Stephen's Day",
                                "name": "Day of Goodwill",
                                "countryCode": "ZA",
                                "fixed": false,
                                "global": true,
                                "counties": null,
                                "launchYear": null,
                                "types": [
                                    "Public"
                                ]
                            }
                        ]
                        """;

        wireMock.register(WireMock.get(WireMock.urlPathEqualTo(uri))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(expectedBody)));
    }


    @Test
    void contextLoads() {
    }


    // ==================== Helper Methods ====================

    protected void stubCapitecApiSuccess( WireMock capitecApiWireMock,String responseBody) {
        capitecApiWireMock.register(
                post(urlPathEqualTo("/branches"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody(responseBody))
        );
    }


    protected void stubCapitecApiEmptyResponse(WireMock capitecApiWireMock,String response) {
        capitecApiWireMock.register(
                post(urlPathEqualTo("/branches"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody(response))
        );
    }

    protected void stubCapitecApiError(WireMock capitecApiWireMock) {
        capitecApiWireMock.register(
                post(urlPathEqualTo("/branches"))
                        .willReturn(aResponse()
                                .withStatus(500)
                                .withHeader("Content-Type", "application/json")
                                .withBody("{\"error\": \"Internal Server Error\"}"))
        );
    }

    protected void stubCapitecApiPersistentFailure(WireMock capitecApiWireMock) {
        capitecApiWireMock.register(
                post(urlPathEqualTo("/branches"))
                        .willReturn(aResponse()
                                .withStatus(503)
                                .withHeader("Content-Type", "application/json")
                                .withBody("{\"error\": \"Service Unavailable\"}"))
        );
    }

    protected void stubCapitecApiFailThenSucceed(WireMock capitecApiWireMock, String response) {
        // First call fails
        capitecApiWireMock.register(
                post(urlPathEqualTo("/branches"))
                        .inScenario("Retry Scenario")
                        .whenScenarioStateIs(Scenario.STARTED)
                        .willReturn(aResponse()
                                .withStatus(503)
                                .withBody("{\"error\": \"Service Unavailable\"}"))
                        .willSetStateTo("RETRY_1")
        );

        // Second call succeeds
        capitecApiWireMock.register(
                post(urlPathEqualTo("/branches"))
                        .inScenario("Retry Scenario")
                        .whenScenarioStateIs("RETRY_1")
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody(response))
        );
    }

    public  static  String capitecApiBranchResponse(){

       return """
               {
                    "branches": [
                      {
                        "id": "SAS293200",
                        "code": "470010",
                        "name": "Rondebosch",
                        "latitude": -33.960553,
                        "longitude": 18.470156,
                        "addressLine1": "Shop G21, Cnr Main & Belmont Road, Fountain Centre, Rondebosch, 7700",
                        "addressLine2": "Fountain Centre",
                        "city": "Rondebosch",
                        "province": "Western Cape",
                        "isAtm": false,
                        "cashAccepting": false,
                        "homeLoan": false,
                        "isClosed": false,
                        "businessBankCenter": false,
                        "operationhours": {
                          "MONDAY": { "openAt": "08:00:00", "closeAt": "17:00:00", "closed": false },
                          "TUESDAY": { "openAt": "08:00:00", "closeAt": "17:00:00", "closed": false },
                          "WEDNESDAY": { "openAt": "08:00:00", "closeAt": "17:00:00", "closed": false },
                          "THURSDAY": { "openAt": "08:00:00", "closeAt": "17:00:00", "closed": false },
                          "FRIDAY": { "openAt": "08:00:00", "closeAt": "17:00:00", "closed": false },
                          "SATURDAY": { "openAt": "08:00:00", "closeAt": "13:00:00", "closed": false },
                          "SUNDAY": { "openAt": null, "closeAt": null, "closed": true },
                          "PUBLIC_HOLIDAY": { "openAt": null, "closeAt": null, "closed": true }
                        }
                      },
                      {
                        "id": "SAS29300",
                        "code": "470020",
                        "name": "Cape Town CBD",
                        "latitude": -33.925839,
                        "longitude": 18.423622,
                        "addressLine1": "Shop 5, Cape Town Station Building, Adderley Street",
                        "addressLine2": null,
                        "city": "Cape Town",
                        "province": "Western Cape",
                        "isAtm": false,
                        "cashAccepting": false,
                        "homeLoan": true,
                        "isClosed": false,
                        "businessBankCenter": true,
                        "operationhours": {
                          "MONDAY": { "openAt": "08:00:00", "closeAt": "17:00:00", "closed": false },
                          "TUESDAY": { "openAt": "08:00:00", "closeAt": "17:00:00", "closed": false },
                          "WEDNESDAY": { "openAt": "08:00:00", "closeAt": "17:00:00", "closed": false },
                          "THURSDAY": { "openAt": "08:00:00", "closeAt": "17:00:00", "closed": false },
                          "FRIDAY": { "openAt": "08:00:00", "closeAt": "17:00:00", "closed": false },
                          "SATURDAY": { "openAt": "08:00:00", "closeAt": "13:00:00", "closed": false },
                          "SUNDAY": { "openAt": null, "closeAt": null, "closed": true },
                          "PUBLIC_HOLIDAY": { "openAt": null, "closeAt": null, "closed": true }
                        }
                      },
                      {
                        "id": "SAS29340",
                        "code": "470030",
                        "name": "Total Rondebosch ATM",
                        "latitude": -25.7751312,
                        "longitude": 29.4944725,
                        "addressLine1": "Total Rondebosch Vulstasie, Corner of N11",
                        "addressLine2": null,
                        "city": "Middelburg",
                        "province": "Mpumalanga",
                        "isAtm": true,
                        "cashAccepting": false,
                        "homeLoan": false,
                        "isClosed": false,
                        "businessBankCenter": false,
                        "operationhours": {
                          "MONDAY": { "openAt": "00:00:00", "closeAt": "23:59:59", "closed": false },
                          "TUESDAY": { "openAt": "00:00:00", "closeAt": "23:59:59", "closed": false },
                          "WEDNESDAY": { "openAt": "00:00:00", "closeAt": "23:59:59", "closed": false },
                          "THURSDAY": { "openAt": "00:00:00", "closeAt": "23:59:59", "closed": false },
                          "FRIDAY": { "openAt": "00:00:00", "closeAt": "23:59:59", "closed": false },
                          "SATURDAY": { "openAt": "00:00:00", "closeAt": "23:59:59", "closed": false },
                          "SUNDAY": { "openAt": "00:00:00", "closeAt": "23:59:59", "closed": false },
                          "PUBLIC_HOLIDAY": { "openAt": "00:00:00", "closeAt": "23:59:59", "closed": false }
                        }
                      },
                      {
                        "id": "SASB9001",
                        "code": "470010",
                        "name": "Century City Business Support",
                        "latitude": -33.884873,
                        "longitude": 18.518016,
                        "addressLine1": "64 Century Boulevard, Century City, Cape Town, 7441, South Africa",
                        "addressLine2": null,
                        "city": "Cape Town",
                        "province": "Western Cape",
                        "isAtm": false,
                        "cashAccepting": false,
                        "homeLoan": false,
                        "isClosed": false,
                        "businessBankCenter": false,
                        "operationhours": {
                          "MONDAY": { "openAt": "08:00:00", "closeAt": "17:00:00", "closed": false },
                          "TUESDAY": { "openAt": "08:00:00", "closeAt": "17:00:00", "closed": false },
                          "WEDNESDAY": { "openAt": "08:00:00", "closeAt": "17:00:00", "closed": false },
                          "THURSDAY": { "openAt": "08:00:00", "closeAt": "17:00:00", "closed": false },
                          "FRIDAY": { "openAt": "08:00:00", "closeAt": "17:00:00", "closed": false },
                          "SATURDAY": { "openAt": "08:00:00", "closeAt": "13:00:00", "closed": false },
                          "SUNDAY": { "openAt": null, "closeAt": null, "closed": true },
                          "PUBLIC_HOLIDAY": { "openAt": null, "closeAt": null, "closed": true }
                        }
                      },
                      {
                        "id": "SASB9002",
                        "code": "470010",
                        "name": "Montague Gardens",
                        "latitude": -33.8793377854504,
                        "longitude": 18.5217539445352,
                        "addressLine1": "John Montague Centre, Montague Drive, Montague Gardens, 7441",
                        "addressLine2": null,
                        "city": "Montague Gardens",
                        "province": "Western Cape",
                        "isAtm": false,
                        "cashAccepting": false,
                        "homeLoan": false,
                        "isClosed": false,
                        "businessBankCenter": false,
                        "operationhours": {
                          "MONDAY": { "openAt": "08:00:00", "closeAt": "17:00:00", "closed": false },
                          "TUESDAY": { "openAt": "08:00:00", "closeAt": "17:00:00", "closed": false },
                          "WEDNESDAY": { "openAt": "08:00:00", "closeAt": "17:00:00", "closed": false },
                          "THURSDAY": { "openAt": "08:00:00", "closeAt": "17:00:00", "closed": false },
                          "FRIDAY": { "openAt": "08:00:00", "closeAt": "17:00:00", "closed": false },
                          "SATURDAY": { "openAt": "08:00:00", "closeAt": "13:00:00", "closed": false },
                          "SUNDAY": { "openAt": null, "closeAt": null, "closed": true },
                          "PUBLIC_HOLIDAY": { "openAt": null, "closeAt": null, "closed": true }
                        }
                      },
                      {
                        "id": "SASB9003",
                        "code": "470010",
                        "name": "Canal Walk",
                        "latitude": -33.89406,
                        "longitude": 18.511311,
                        "addressLine1": "Shop 171, Century Boulevard, Canal Walk Shopping Centre, Century City, Cape Town, 7441",
                        "addressLine2": "Canal Walk",
                        "city": "Cape Town",
                        "province": "Western Cape",
                        "isAtm": false,
                        "cashAccepting": true,
                        "homeLoan": true,
                        "isClosed": false,
                        "businessBankCenter": false,
                        "operationhours": {
                          "MONDAY": { "openAt": "09:00:00", "closeAt": "18:00:00", "closed": false },
                          "TUESDAY": { "openAt": "09:00:00", "closeAt": "18:00:00", "closed": false },
                          "WEDNESDAY": { "openAt": "09:00:00", "closeAt": "18:00:00", "closed": false },
                          "THURSDAY": { "openAt": "09:00:00", "closeAt": "18:00:00", "closed": false },
                          "FRIDAY": { "openAt": "09:00:00", "closeAt": "18:00:00", "closed": false },
                          "SATURDAY": { "openAt": "09:00:00", "closeAt": "15:00:00", "closed": false },
                          "SUNDAY": { "openAt": "09:00:00", "closeAt": "13:00:00", "closed": false },
                          "PUBLIC_HOLIDAY": { "openAt": null, "closeAt": null, "closed": true }
                        }
                      },
                      {
                        "id": "SASB9004",
                        "code": "470010",
                        "name": "Goodwood N1 City",
                        "latitude": -33.8932973250128,
                        "longitude": 18.5585940598494,
                        "addressLine1": "Shop 40a and 41, N1 City, Louwtjie Rothman Street, Goodwood, 7460",
                        "addressLine2": "N1 City",
                        "city": "Goodwood",
                        "province": "Western Cape",
                        "isAtm": false,
                        "cashAccepting": false,
                        "homeLoan": true,
                        "isClosed": false,
                        "businessBankCenter": false,
                        "operationhours": {
                          "MONDAY": { "openAt": "09:00:00", "closeAt": "18:00:00", "closed": false },
                          "TUESDAY": { "openAt": "09:00:00", "closeAt": "18:00:00", "closed": false },
                          "WEDNESDAY": { "openAt": "09:00:00", "closeAt": "18:00:00", "closed": false },
                          "THURSDAY": { "openAt": "09:00:00", "closeAt": "18:00:00", "closed": false },
                          "FRIDAY": { "openAt": "09:00:00", "closeAt": "18:00:00", "closed": false },
                          "SATURDAY": { "openAt": "09:00:00", "closeAt": "14:00:00", "closed": false },
                          "SUNDAY": { "openAt": "09:00:00", "closeAt": "13:00:00", "closed": false },
                          "PUBLIC_HOLIDAY": { "openAt": null, "closeAt": null, "closed": true }
                        }
                      },
                      {
                        "id": "SASB9005",
                        "code": "470010",
                        "name": "Goodwood Mall",
                        "latitude": -33.910978,
                        "longitude": 18.552359,
                        "addressLine1": "Shop 8A, 8B & 9, Goodwood Mall, cnr Mc Donald & Voortrekker Road, Goodwood, 7460",
                        "addressLine2": "Goodwood Mall",
                        "city": "Goodwood",
                        "province": "Western Cape",
                        "isAtm": false,
                        "cashAccepting": false,
                        "homeLoan": false,
                        "isClosed": false,
                        "businessBankCenter": false,
                        "operationhours": {
                          "MONDAY": { "openAt": "09:00:00", "closeAt": "18:00:00", "closed": false },
                          "TUESDAY": { "openAt": "09:00:00", "closeAt": "18:00:00", "closed": false },
                          "WEDNESDAY": { "openAt": "09:00:00", "closeAt": "18:00:00", "closed": false },
                          "THURSDAY": { "openAt": "09:00:00", "closeAt": "18:00:00", "closed": false },
                          "FRIDAY": { "openAt": "09:00:00", "closeAt": "18:00:00", "closed": false },
                          "SATURDAY": { "openAt": "08:00:00", "closeAt": "13:00:00", "closed": false },
                          "SUNDAY": { "openAt": null, "closeAt": null, "closed": true },
                          "PUBLIC_HOLIDAY": { "openAt": null, "closeAt": null, "closed": true }
                        }
                      }
                    ]
                  }
            """;
    }
    public  static  String capitecApiBranchEmptyResponse() {
       return  """
            {
                "Branches": []
            }
            """;
    }
}
