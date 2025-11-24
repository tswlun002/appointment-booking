package lunga.appointmentbooking.user.infrastructure.controller;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import lunga.appointmentbooking.AppointmentBookingApplicationTests;
import lunga.appointmentbooking.authentication.domain.TokenResponse;
import lunga.appointmentbooking.exeption.AppException;
import lunga.appointmentbooking.keycloak.domain.KeycloakService;
import lunga.appointmentbooking.otp.domain.OTPService;
import lunga.appointmentbooking.user.app.NewUserDtO;
import lunga.appointmentbooking.user.domain.User;
import lunga.appointmentbooking.user.domain.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class UserAuthControllerTest extends AppointmentBookingApplicationTests {



    public final static String username = "admin";
    @Autowired
    private KeycloakService keycloakService;
    private RestClient restClientForController;
    @Autowired
    private OTPService otpService;
    @Autowired
    private UserService userService;

    @LocalServerPort
    private int port;




    @BeforeEach
    void setUp() {
        restClientForController = RestClient.builder().baseUrl(String.format("http://localhost:%s/%s",port,"users-service")).build();
    }

    @AfterEach
    void tearDown() {
        UsersResource users = keycloakService.getUsersResources();
        users.list().stream().filter(u-> !Objects.equals(u.getUsername(), username)).map(UserRepresentation::getId).forEach(users::delete);
    }
    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {"Muhammed;Islam;muhammed@myuct.ac.za;@Muhammed2025"})
    public  void registerUser(String firstname, String lastName,String email, String password) {
        NewUserDtO newUserDtO = new NewUserDtO( email, password,firstname, lastName);
        ResponseEntity<String> exchange = restClientForController.post()
                .uri("/auth/register")
                .headers(h -> h.add("Trace-Id", UUID.randomUUID().toString()))
                .body(newUserDtO)
                .exchange((_, res) ->
                        new ResponseEntity<>(Objects.requireNonNull(res.bodyTo(String.class)),res.getStatusCode())
                );
        assertThat(exchange.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {"Muhammed;Islam;muhammed@cput.ac.za;@Muhammed2025"})
    public  void verifyUser(String firstname, String lastName,String email, String password) throws IOException {
        NewUserDtO newUserDtO = new NewUserDtO( email, password,firstname, lastName);
        restClientForController.post()
                .uri("/auth/register")
                .headers(h -> h.add("Trace-Id", UUID.randomUUID().toString()))
                .body(newUserDtO)
                .exchange((_, res) ->
                        new ResponseEntity<>(Objects.requireNonNull(res.bodyTo(String.class)),res.getStatusCode())
                );

        User user = userService.getUserByEmail(email).orElseThrow();
        var otpEntity = otpService.find(user.getUsername()).stream().sorted((a, b) -> b.getCreationDate().compareTo(a.getCreationDate()))
                .findFirst().orElseThrow();;
        VerificationDTO verificationDTO = new VerificationDTO(email, otpEntity.getCode());
        ResponseEntity<Map<String, JsonNode>> exchange = restClientForController.put()
                .uri("/auth/verify")
                .headers(h -> h.add("Trace-Id", UUID.randomUUID().toString()))
                .body(verificationDTO)
                .exchange((req, res) -> {

                            log.info("response: {} {}", res.getStatusCode(), res.getStatusText());
                            return new ResponseEntity<>(Objects.requireNonNull(res.bodyTo(Map.class)), res.getStatusCode());
                        }
                );
        Assertions.assertNotNull(exchange);
        assertThat(exchange.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(exchange.getBody()).isNotNull();
        TokenResponse tokenResponse = new ObjectMapper().convertValue(exchange.getBody(), TokenResponse.class);
        assertThat(tokenResponse).isNotNull();
        assertThat(tokenResponse.getRefreshToken()).isNull();
        assertThat(tokenResponse.getIdToken()).isNotNull();
        assertThat(tokenResponse.getToken()).isNotNull();
        User user1 = userService.getUserByEmail(email).orElseThrow();
        assertThat(user1).hasFieldOrPropertyWithValue("email", email)
                .hasFieldOrPropertyWithValue("enabled", true)
                .hasFieldOrPropertyWithValue("verified", true);
    }

    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {"Muhammed;Islam;muhammed@cput.ac.za;@Muhammed2025"})
    public  void verifyUserWithInvalidOTPUntilDisable(String firstname, String lastName,String email, String password) {
        NewUserDtO newUserDtO = new NewUserDtO( email, password,firstname, lastName);
        restClientForController.post()
                .uri("/auth/register")
                .headers(h -> h.add("Trace-Id", UUID.randomUUID().toString()))
                .body(newUserDtO)
                .exchange((_, res) ->
                        new ResponseEntity<>(Objects.requireNonNull(res.bodyTo(String.class)),res.getStatusCode())
                );

        // User user = userService.getUserByEmail(email).orElseThrow();
        //OTPEntity otpEntity = otpService.find(user.getUsername()).orElseThrow();
        VerificationDTO verificationDTO = new VerificationDTO(email, "123456");
        IntStream.range(1,4).forEach(i-> {
            ResponseEntity<AppException> exchange = restClientForController.put()
                    .uri("/auth/verify")
                    .headers(h -> h.add("Trace-Id", UUID.randomUUID().toString()))
                    .body(verificationDTO)
                    .exchange((req, res) -> {
                                //if(i!=3) return new ResponseEntity<>(res.bodyTo(String.class),res.getStatusCode());
                                return new ResponseEntity<>(Objects.requireNonNull(res.bodyTo(AppException.class)), res.getStatusCode());
                            }

                    );
            if(i<3){

                Assertions.assertNotNull(exchange);
                assertThat(exchange.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                assertThat(exchange.getBody()).hasFieldOrPropertyWithValue("statusCodeMessage",HttpStatus.FORBIDDEN.name())
                        .hasFieldOrPropertyWithValue("status",HttpStatus.FORBIDDEN.value())
                        .hasFieldOrPropertyWithValue("message","Failed, OTP is invalid")
                        .hasFieldOrPropertyWithValue("path","/users-service/auth/verify");
                User user = userService.getUserByEmail(email).orElseThrow();
                assertThat(user).hasFieldOrPropertyWithValue("email", email)
                        .hasFieldOrPropertyWithValue("enabled", true)
                        .hasFieldOrPropertyWithValue("verified", false);
            }
            else{
                Assertions.assertNotNull(exchange);
                assertThat(exchange.getStatusCode()).isEqualTo(HttpStatus.LOCKED);
                assertThat(exchange.getBody()).hasFieldOrPropertyWithValue("statusCodeMessage",HttpStatus.LOCKED.name())
                        .hasFieldOrPropertyWithValue("status",HttpStatus.LOCKED.value())
                        .hasFieldOrPropertyWithValue("message","Too many attempt to verify OTP, please try 5 hours later.")
                        .hasFieldOrPropertyWithValue("path","/users-service/auth/verify");

                User user = userService.getUserByEmail(email).orElseThrow();
                assertThat(user).hasFieldOrPropertyWithValue("email", email)
                        .hasFieldOrPropertyWithValue("enabled", false)
                        .hasFieldOrPropertyWithValue("verified", false);
            }
        });
    }

}