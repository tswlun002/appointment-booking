package capitec.branch.appointment.user.app;


import jakarta.ws.rs.NotFoundException;
import capitec.branch.appointment.AppointmentBookingApplicationTests;
import capitec.branch.appointment.otp.domain.OTP;
import capitec.branch.appointment.otp.domain.OTPService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class DeleteUserUseCaseTest extends AppointmentBookingApplicationTests {

    @Autowired
    RegistrationUserCase registrationUserCase;
    @Autowired
    OTPService otpService;
    @Autowired
    DeleteUserUseCase deleteUserUseCase;

    @AfterEach
    void tearUp() {
        otpService.deleteAllOTP("f9ad5e5b-f4f8-42e0-bb93-26b283e6f55d");

    }

    @ParameterizedTest
    @CsvSource(delimiter = ';',
            value = {
                    "alicelei@myuct.ac.za;AliceLei;XiaolingFerreira;@KrVgfjl65;78d4517d-d110-4bc4-96b3-ea77f4283cf4",
                    "gopalflores1@cput.ac.za;Gopal;Flores;1wcB2OsQFV6_;e23f32b9-3dea-41ed-ac8d-fa283dacb424"
            })
    void deleteExistingUserRequest(String email, String firstname, String lastname, String password, String traceId) {
        var userRegister = new NewUserDtO(email, password, firstname, lastname);
        var user = registrationUserCase.registerUser(userRegister, traceId);
        boolean isDeleted = deleteUserUseCase.deleteUserRequest(user.getUsername(), password, traceId);
        assertThat(isDeleted).isTrue();
    }

    @ParameterizedTest
    @CsvSource(delimiter = ';',
            value = {
                    "alicelei@myuct.ac.za;AliceLei;XiaolingFerreira;@KrVgfjl65;78d4517d-d110-4bc4-96b3-ea77f4283cf4",
                    "gopalflores1@cput.ac.za;Gopal;Flores;1wcB2OsQFV6_;e23f32b9-3dea-41ed-ac8d-fa283dacb424"
            })
    void deleteExistingUser(String email, String firstname, String lastname, String password, String traceId) {
        var userRegister = new NewUserDtO(email, password, firstname, lastname);
        var user = registrationUserCase.registerUser(userRegister, traceId);
        OTP otp1 = otpService.find(user.getUsername()).stream().sorted((a, b) -> b.getCreationDate().compareTo(a.getCreationDate()))
                .findFirst().orElseThrow();
        otpService.validateOTP(user.getUsername(),otp1.getCode(),3);
        deleteUserUseCase.deleteUserRequest(user.getUsername(), password, traceId);
        OTP otp = otpService.find(user.getUsername()).stream().sorted((a, b) -> b.getCreationDate().compareTo(a.getCreationDate()))
                .findFirst().orElseThrow();
        boolean isDeleted = deleteUserUseCase.deleteUser(user.getUsername(), otp.getCode(), traceId);
        assertThat(isDeleted).isTrue();
        assertThatThrownBy(() -> registrationUserCase.getUser(user.getUsername()))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User is not found");

        Optional<OTP> otpEntity = otpService.find(user.getUsername()).stream().sorted((a, b) -> b.getCreationDate().compareTo(a.getCreationDate()))
                .findFirst();
        assertThat(otpEntity.isEmpty()).isTrue();
    }
}