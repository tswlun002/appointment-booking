package capitec.branch.appointment.otp.app;

import capitec.branch.appointment.AppointmentBookingApplicationTests;
import capitec.branch.appointment.exeption.OTPExpiredException;
import capitec.branch.appointment.otp.domain.OTP;
import capitec.branch.appointment.otp.domain.OTPPurpose;
import capitec.branch.appointment.otp.domain.OTPService;
import capitec.branch.appointment.otp.domain.OTPStatus;
import capitec.branch.appointment.sharekernel.username.UsernameGenerator;
import capitec.branch.appointment.user.infrastructure.keycloak.KeycloakUserCacheConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


class ValidateOTPUseCaseTest extends AppointmentBookingApplicationTests {

    public final static String username = "admin";


    @Autowired
    private OTPService otpService;

    @Autowired
    private  CreateOTPUseCase createOTPUseCase;
    @Autowired
    private ValidateOTPUseCase validateOTPUseCase;

    @Autowired
    @Qualifier(KeycloakUserCacheConfig.KEYCLOAK_USER_CACHE_MANAGER)
    private CacheManager cacheManager;

    private final String USER_REF = new UsernameGenerator().getId();
    private final String trace_id = "f9ad5e5b-f4f8-42e0-bb93-26b283e6f55d";

    @AfterEach
    void tearUp() {

        var cache = new Cache[]{
                cacheManager.getCache(KeycloakUserCacheConfig.KEYCLOAK_USER_CACHE),
                cacheManager.getCache(KeycloakUserCacheConfig.KEYCLOAK_USER_CACHE_MANAGER)
        };
        for (Cache cache1 : cache) {
            if(cache1!=null) {
                cache1.clear();
            }
        }
        otpService.find(USER_REF)
                .stream().map(OTP::getUsername)
                .forEach(otpService::deleteUserOTP);
    }

    @Test
    void testValidate_OTP_Successfully() {
        CreateOtpDto dto = new CreateOtpDto(USER_REF, trace_id, OTPPurpose.EMAIL_VERIFICATION);
        OTP execute = createOTPUseCase.execute(dto);
        validateOTPUseCase.validateOTP(execute.getUsername(),execute.getCode(), "1d022a43-6fc7-4314-a654-7452c549f25b");
        Optional<OTP> optional = otpService.findLatestOTP(execute.getUsername(), LocalDateTime.now().minusHours(2));
        assertThat(optional.isPresent()).isTrue();
        OTP otp = optional.get();
        assertThat(otp.getUsername()).isEqualTo(execute.getUsername());
        assertThat(otp.getCode()).isEqualTo(execute.getCode());
        assertThat(otp.getStatus()).isEqualTo(OTPStatus.VALIDATED);
        assertThat(otp.getVersion()).isEqualTo(1);

    }

    @Test
    void testValidate_OTP_AlreadyValidated_ThrowResponseStatusException() {
        CreateOtpDto dto = new CreateOtpDto(USER_REF, trace_id, OTPPurpose.EMAIL_VERIFICATION);
        OTP execute = createOTPUseCase.execute(dto);
        validateOTPUseCase.validateOTP(execute.getUsername(),execute.getCode(), "1d022a43-6fc7-4314-a654-7452c549f25b");
        Optional<OTP> optional = otpService.findLatestOTP(execute.getUsername(), LocalDateTime.now().minusHours(2));
       assertThatThrownBy(()-> validateOTPUseCase.validateOTP(execute.getUsername(),execute.getCode(), "1d022a43-6fc7-4314-a654-7452c549f25b"))
               .isInstanceOf(ResponseStatusException.class)
               .hasMessageContaining("OTP is already verified");
        assertThat(optional.isPresent()).isTrue();
        Optional<OTP> latestOTP = otpService.findLatestOTP(execute.getUsername(), LocalDateTime.now().minusHours(2));
        assertThat(latestOTP.isPresent()).isTrue();
        assertThat(latestOTP.get().getVersion()).isEqualTo(1);
        assertThat(latestOTP.get().getStatus()).isEqualTo(OTPStatus.VALIDATED);
    }

    @Test
    void testValidate_RevokedOTP_ThrowResponseStatusException() {
        CreateOtpDto dto = new CreateOtpDto(USER_REF, trace_id, OTPPurpose.EMAIL_VERIFICATION);
        OTP revoked = createOTPUseCase.execute(dto);
        revoked.revoke();
        // persist revoke status
        boolean update = otpService.update(revoked, OTPStatus.CREATED);
        assertThat(update).isTrue();

        revoked = otpService.findLatestOTP(revoked.getUsername(), LocalDateTime.now().minusHours(2)).get();
        assertThat(revoked.getVersion()).isEqualTo(1);

        OTP finalRevoked = revoked;
        assertThatThrownBy(()-> validateOTPUseCase.validateOTP(finalRevoked.getUsername(), finalRevoked.getCode(), "11654576-4ed1-4232-bae7-afe231a36e09"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Too many attempt to verify OTP, please try 5 hours later.");

        Optional<OTP> latestOTP = otpService.findLatestOTP(revoked.getUsername(), LocalDateTime.now().minusHours(2));
        assertThat(latestOTP.isPresent()).isTrue();
        assertThat(latestOTP.get().getVersion()).isEqualTo(1);
        assertThat(latestOTP.get().getStatus()).isEqualTo(OTPStatus.REVOKED);
    }
    @Test
    void testValidate_UntilMaxAttempts_ThrowResponseStatusException() {
        CreateOtpDto dto = new CreateOtpDto(USER_REF, trace_id, OTPPurpose.EMAIL_VERIFICATION);
        OTP newOTP = createOTPUseCase.execute(dto);

       var result= validateOTPUseCase.validateOTP(newOTP.getUsername(), newOTP.getCode()+"#", "11654576-4ed1-4232-bae7-afe231a36e09");
       assertThat(result).isFalse();
       result= validateOTPUseCase.validateOTP(newOTP.getUsername(), newOTP.getCode()+"$", "6619d3bc-a659-448e-a25d-aacecce50f7e");
        assertThat(result).isFalse();


        assertThatThrownBy(()-> validateOTPUseCase.validateOTP(newOTP.getUsername(), newOTP.getCode()+"^", "cbdb6f8b-2bb3-4bc4-9daf-6e6e63c0b844"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Too many attempt to verify OTP, please try 5 hours later.");

        Optional<OTP> latestOTP = otpService.findLatestOTP(newOTP.getUsername(), LocalDateTime.now().minusHours(2));
        assertThat(latestOTP.isPresent()).isTrue();
        assertThat(latestOTP.get().getVersion()).isEqualTo(3);
        assertThat(latestOTP.get().getStatus()).isEqualTo(OTPStatus.REVOKED);
    }

    @Test
    void testValidate_expiredOTP_getNewOTP_Successfully() throws InterruptedException {
        CreateOtpDto dto = new CreateOtpDto(USER_REF, trace_id, OTPPurpose.EMAIL_VERIFICATION);
        OTP newOTP = createOTPUseCase.execute(dto);
         Thread.sleep(Duration.of(6, ChronoUnit.SECONDS));

        assertThatThrownBy(()-> validateOTPUseCase.validateOTP(newOTP.getUsername(), newOTP.getCode(), "cbdb6f8b-2bb3-4bc4-9daf-6e6e63c0b844"))
                .isInstanceOf(OTPExpiredException.class);

        Optional<OTP> latestOTP = otpService.findLatestOTP(newOTP.getUsername(), LocalDateTime.now().minusHours(2));
        assertThat(latestOTP.isPresent()).isTrue();
        assertThat(latestOTP.get().getVersion()).isEqualTo(0);
        assertThat(latestOTP.get().getStatus()).isEqualTo(OTPStatus.RENEWED);
    }


}