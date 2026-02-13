package capitec.branch.appointment.otp.app;

import capitec.branch.appointment.exeption.OptimisticLockConflictException;
import capitec.branch.appointment.otp.domain.OTP;
import capitec.branch.appointment.otp.domain.OTPService;
import capitec.branch.appointment.otp.domain.VerificationAttempts;
import capitec.branch.appointment.utils.UseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

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
    @Value("${otp.expire.chron-units:MINUTES}")
    private ChronoUnit expireTimeUnit;

    public OTP execute(@Valid CreateOtpDto dto) {


        log.info("Creating registration OTP for user: {}, traceId: {}", dto.purpose(), dto.traceId());

        try {
            var verificationAttempts = new VerificationAttempts(0, maxVerificationAttempts);

            OTP otp = new OTP(
                    dto.username(),
                    LocalDateTime.now().plus(Duration.of(expireDatetime, expireTimeUnit)),
                    dto.purpose(),
                    verificationAttempts,
                    expireTimeUnit
            );

            return otpService.saveOTP(otp);

        } catch (IllegalArgumentException | IllegalStateException e) {

            log.error("Invalid  OTP  variable: {}",e.getMessage(), e);
            throw  new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(),e);

        } catch (OptimisticLockConflictException e) {

            log.info("OTP already exists for user: {}, traceId: {}", dto.purpose(), dto.traceId());
            throw  new ResponseStatusException(HttpStatus.CONFLICT,"OTP is being multiple process. Please try again later",e);
        }
        catch (Exception e) {
            log.error("Unexpected error: {}",e.getMessage(),e);
            throw  new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Failed to create OTP. Please try again later",e);
        }

    }
}
