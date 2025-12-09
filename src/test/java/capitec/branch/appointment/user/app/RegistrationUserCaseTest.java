package capitec.branch.appointment.user.app;

import capitec.branch.appointment.AppointmentBookingApplicationTests;
import capitec.branch.appointment.authentication.domain.TokenResponse;
import capitec.branch.appointment.event.domain.RecordStatus;
import capitec.branch.appointment.event.domain.UserDeadLetterService;
import capitec.branch.appointment.exeption.EntityAlreadyExistException;
import capitec.branch.appointment.kafka.user.UserDefaultErrorEvent;
import capitec.branch.appointment.keycloak.domain.KeycloakService;
import capitec.branch.appointment.otp.domain.OTP;
import capitec.branch.appointment.otp.domain.OTPSTATUSENUM;
import capitec.branch.appointment.otp.domain.OTPService;
import capitec.branch.appointment.otp.domain.OTPStatus;
import capitec.branch.appointment.user.domain.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;


class RegistrationUserCaseTest extends AppointmentBookingApplicationTests {

    public final static String username = "admin";
    @Autowired
    RegistrationUserCase registrationUserCase;
    @Autowired
    OTPService otpService;
    @Autowired
    private UserDeadLetterService userDeadLetterService;
    @Autowired
    private KeycloakService keycloakService;



    @AfterEach
    void tearUp() {
        otpService.deleteAllOTP("f9ad5e5b-f4f8-42e0-bb93-26b283e6f55d");
        UsersResource usersResource = keycloakService.getUsersResources();
        List<UserRepresentation> list = usersResource.list().stream().filter(u->!u.getUsername().equals(username)).toList();
        list.stream().map(UserRepresentation::getId).forEach(usersResource::delete);

    }


    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {"alicelei@myuct.ac.za;AliceLei;XiaolingFerreira;@KrVgfjl65;78d4517d-d110-4bc4-96b3-ea77f4283cf4",
            "gopalflores1@cput.ac.za;Gopal;Flores;1wcB2OsQFV6_;e23f32b9-3dea-41ed-ac8d-fa283dacb424"})
    void testRegisterUser(String email, String firstname, String lastname, String password, String traceId) {

        var userRegister = new NewUserDtO(email, firstname, lastname,password);

        var registeredUser = registrationUserCase.registerUser(userRegister, traceId);

        var failedRecord = userDeadLetterService.findByStatus(RecordStatus.DEAD);

        assertThat(failedRecord.stream().noneMatch(r -> {
            String value = r.getValue();
            return r.getTraceId().equals(traceId) &&
                    value.contains(String.valueOf(registeredUser.getUsername()))
                    && value.contains(registeredUser.getEmail());
        })).isTrue();

    }

    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {"alicelei@myuct.ac.za;AliceLei;XiaolingFerreira;@KrVgfjl65;9507037886081;78d4517d-d110-4bc4-96b3-ea77f4283cf4",
            "gopalflores1@cput.ac.za;Gopal;Flores;1wcB2OsQFV6_;9607037886182;e23f32b9-3dea-41ed-ac8d-fa283dacb424"})
    void testRegisterCapitecClient(String email, String firstname, String lastname, String password,String idNumber, String traceId) {

        var userRegister = new NewUserDtO(email, password, firstname, lastname, idNumber, true);

        User user = new User(userRegister.email(), userRegister.firstname(), userRegister.lastname(), userRegister.password());
        wireMockGetHolidayByYearAndCountryCode(user, idNumber);
        var registeredUser = registrationUserCase.registerUser(userRegister, traceId);

        var failedRecord = userDeadLetterService.findByStatus(RecordStatus.DEAD);

        assertThat(failedRecord.stream().noneMatch(r -> {
            String value = r.getValue();
            return r.getTraceId().equals(traceId) &&
                    value.contains(String.valueOf(registeredUser.getUsername()))
                    && value.contains(registeredUser.getEmail());
        })).isTrue();

    }

    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {"alicelei@myuct.ac.za;AliceLei;XiaolingFerreira;@KrVgfjl65;78d4517d-d110-4bc4-96b3-ea77f4283cf4",
            "gopalflores1@cput.ac.za;Gopal;Flores;1wcB2OsQFV6_;e23f32b9-3dea-41ed-ac8d-fa283dacb424"})
    void testRegisterDuplicateUser(String email, String firstname, String lastname, String password, String traceId) {

        var userRegister = new NewUserDtO(email, firstname, lastname,password);
        registrationUserCase.registerUser(userRegister, traceId);
        assertThatThrownBy(() -> registrationUserCase.registerUser(userRegister, traceId))
                .isInstanceOf(EntityAlreadyExistException.class)
                .hasMessageContaining("User already exists");


    }

    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {"dorismia@myuct.ac.za;Doris;Mia;@KrVgfjl65;2b1d5b0d-59e2-47ec-94f1-24505ef2abbd",
            "davidchong@cput.ac.za;David;Chong;1wcB2OsQFV6_;03289819-bcf4-4fef-8747-cfaf4d1e808a"})
    void testVerifyRegisteredGuestUser(String email, String firstname, String lastname,
                                  String password, String traceId) {

        var registerDTO = new NewUserDtO(email, firstname, lastname,password);
        User user = registrationUserCase.registerUser(registerDTO, traceId);
        var otp = otpService.find(user.getUsername()).stream().sorted((a, b) -> b.getCreationDate().compareTo(a.getCreationDate()))
                .findFirst().orElseThrow();

        TokenResponse tokenResponse = registrationUserCase.verifyUser(user.getUsername(), otp.getCode(), traceId);

        assertThat(tokenResponse).isNotNull();

        var verifiedUser = registrationUserCase.getUser(user.getUsername());
        assertThat(verifiedUser)
                .hasFieldOrPropertyWithValue("email", email)
                .hasFieldOrPropertyWithValue("firstname", firstname)
                .hasFieldOrPropertyWithValue("lastname", lastname)
                .hasFieldOrPropertyWithValue("verified", true)
                .hasFieldOrPropertyWithValue("enabled", true);

        var failedRecord = userDeadLetterService.findByStatus(RecordStatus.DEAD);
        assertThat(failedRecord.stream().noneMatch(r -> {
            String value = r.getValue();
            return r.getTraceId().equals(traceId) &&
                    value.contains(String.valueOf(user.getUsername()))
                    && value.contains(user.getEmail());
        })).isTrue();
    }

    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {"dorismia@myuct.ac.za;Doris;Mia;@KrVgfjl65;9707037886182;2b1d5b0d-59e2-47ec-94f1-24505ef2abbd",
            "davidchong@cput.ac.za;David;Chong;1wcB2OsQFV6_;9607037886182;03289819-bcf4-4fef-8747-cfaf4d1e808a"})
    void testVerifyRegisteredCapitecClientUser(String email, String firstname, String lastname,
                                  String password,String idNumber, String traceId) {

        var registerDTO = new NewUserDtO(email, firstname, lastname,password);
        User userMock = new User(registerDTO.email(), registerDTO.firstname(), registerDTO.lastname(), registerDTO.password());
        wireMockGetHolidayByYearAndCountryCode(userMock,idNumber );

        User user = registrationUserCase.registerUser(registerDTO, traceId);
        var otp = otpService.find(user.getUsername()).stream().sorted((a, b) -> b.getCreationDate().compareTo(a.getCreationDate()))
                .findFirst().orElseThrow();

        TokenResponse tokenResponse = registrationUserCase.verifyUser(user.getUsername(), otp.getCode(), traceId);

        assertThat(tokenResponse).isNotNull();

        var verifiedUser = registrationUserCase.getUser(user.getUsername());
        assertThat(verifiedUser)
                .hasFieldOrPropertyWithValue("email", email)
                .hasFieldOrPropertyWithValue("firstname", firstname)
                .hasFieldOrPropertyWithValue("lastname", lastname)
                .hasFieldOrPropertyWithValue("verified", true)
                .hasFieldOrPropertyWithValue("enabled", true);

        var failedRecord = userDeadLetterService.findByStatus(RecordStatus.DEAD);
        assertThat(failedRecord.stream().noneMatch(r -> {
            String value = r.getValue();
            return r.getTraceId().equals(traceId) &&
                    value.contains(String.valueOf(user.getUsername()))
                    && value.contains(user.getEmail());
        })).isTrue();
    }


    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {"dorismia@myuct.ac.za;Doris;Mia;@KrVgfjl65;2b1d5b0d-59e2-47ec-94f1-24505ef2abbd",
            "davidchong@cput.ac.za;David;Chong;1wcB2OsQFV6_;03289819-bcf4-4fef-8747-cfaf4d1e808a"})
    void testVerifyRegisteredUserUntilOTPRevoked(String email, String firstname, String lastname,
                                                 String password, String traceId) {

        var registerDTO = new NewUserDtO(email, firstname, lastname,password);
        User user = registrationUserCase.registerUser(registerDTO, traceId);

        OTP otp1 = otpService.find(user.getUsername()).stream().sorted((a, b) -> b.getCreationDate().compareTo(a.getCreationDate()))
                .findFirst().orElseThrow();

        var otp = otpService.find(user.getUsername(),otp1.getCode()).orElseThrow();

        OTP finalOtp1 = otp;
        assertThatThrownBy(()->registrationUserCase.verifyUser(user.getUsername(), finalOtp1.getCode()+"+", "fef516c1-bd53-4ae1-b2bf-f18cfc0071c9"))
                .isInstanceOf(ResponseStatusException.class);;

        OTP finalOtp = otp;
        assertThatThrownBy(()->registrationUserCase.verifyUser(user.getUsername(), finalOtp.getCode()+"#", "b36948d9-e735-4313-9492-aef05e9c4228"))
                .isInstanceOf(ResponseStatusException.class);

        assertThatThrownBy(()-> registrationUserCase.verifyUser(user.getUsername(), "25hgsf", "7297ef81-6bdd-4956-8e29-a17940971db8"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Too many attempt to verify OTP");
        var verifiedUser = registrationUserCase.getUser(user.getUsername());
        assertThat(verifiedUser)
                .hasFieldOrPropertyWithValue("email", email)
                .hasFieldOrPropertyWithValue("firstname", firstname)
                .hasFieldOrPropertyWithValue("lastname", lastname)
                .hasFieldOrPropertyWithValue("verified", false)
                .hasFieldOrPropertyWithValue("enabled", false);

        otp = otpService.find(user.getUsername(),otp.getCode()).orElseThrow();
        assertThat(otp.getStatus()).isEqualTo(new OTPStatus(OTPSTATUSENUM.REVOKED.name()));
        var failedRecord = userDeadLetterService.findByStatus(RecordStatus.DEAD).stream().map(e->(UserDefaultErrorEvent)e);
        assertThat(failedRecord.noneMatch(r -> r.getTraceId().equals(traceId) &&
                r.getUsername().equals(String.valueOf(user.getUsername()))
                && r.getEmail().equals(user.getEmail()))).isTrue();
    }



}