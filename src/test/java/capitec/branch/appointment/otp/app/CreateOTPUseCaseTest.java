package capitec.branch.appointment.otp.app;


import capitec.branch.appointment.AppointmentBookingApplicationTests;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CreateOTPUseCaseTest extends AppointmentBookingApplicationTests {

    public final static String username = "admin";


    @Autowired
     private OTPService otpService;

    @Autowired
    private  CreateOTPUseCase createOTPUseCase;

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
    void testCreate_NewOTP_Successfully() {

        CreateOtpDto dto = new CreateOtpDto(USER_REF, trace_id, OTPPurpose.EMAIL_VERIFICATION);
        OTP execute = createOTPUseCase.execute(dto);
        assertThat(execute).isNotNull();
        assertThat(execute.getUsername()).isEqualTo(USER_REF);
        assertThat(execute.getCode()).isNotNull().hasSize(OTP.CODE_FIELD_LENGTH);
        assertThat(execute.getPurpose()).isEqualTo(OTPPurpose.EMAIL_VERIFICATION);
        assertThat(execute.getStatus()).isEqualTo(OTPStatus.CREATED);
        assertThat(execute.getVerificationAttempts().attempts()).isEqualTo(0);
    }

    @Test
    void testCreate_NewOTP_WhenUser_HasActiveOTP_Successfully() {

        CreateOtpDto dto = new CreateOtpDto(USER_REF, trace_id, OTPPurpose.EMAIL_VERIFICATION);
        OTP execute = createOTPUseCase.execute(dto);
        CreateOtpDto dto2 = new CreateOtpDto(USER_REF, trace_id, OTPPurpose.EMAIL_VERIFICATION);
        OTP execute2 = createOTPUseCase.execute(dto2);
        assertThat(execute2).isNotNull();
        assertThat(execute2.getUsername()).isEqualTo(USER_REF);
        assertThat(execute2.getCode()).isNotNull().hasSize(OTP.CODE_FIELD_LENGTH);
        assertThat(execute2.getPurpose()).isEqualTo(OTPPurpose.EMAIL_VERIFICATION);
        assertThat(execute2.getStatus()).isEqualTo(OTPStatus.CREATED);
        assertThat(execute2.getVerificationAttempts().attempts()).isEqualTo(0);

        Optional<OTP> otp = otpService.find(execute.getUsername(), execute.getCode(),OTPStatus.REVOKED);
        assertThat(otp).isPresent();
        assertThat(otp.get().getCode()).isEqualTo(execute.getCode());
        assertThat(otp.get().getStatus()).isEqualTo(OTPStatus.REVOKED);
    }

}