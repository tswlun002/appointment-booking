package capitec.branch.appointment.otp.app;

import capitec.branch.appointment.otp.domain.OTP;
import capitec.branch.appointment.otp.domain.OTPService;
import capitec.branch.appointment.otp.domain.VerificationAttempts;
import capitec.branch.appointment.utils.UseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;

@Slf4j
@UseCase
@Validated
@RequiredArgsConstructor
public class CreateOTPUseCase {

    private final OTPService otpService;

    @Value("${otp.number.verification.attempts:2}")
    private int maxVerificationAttempts;

    @Value("${otp.expire.datetime}")
    private long expireDatetime;

    public OTP execute(@Valid CreateOtpDto dto) {


        log.info("Creating registration OTP for user: {}, traceId: {}", dto.purpose(), dto.traceId());
        
        var verificationAttempts = new VerificationAttempts(0, maxVerificationAttempts);
        return otpService.saveOTP(new OTP(
                dto.username(),
                expireDatetime,
                dto.purpose(),
                verificationAttempts
        ));

    }
}
