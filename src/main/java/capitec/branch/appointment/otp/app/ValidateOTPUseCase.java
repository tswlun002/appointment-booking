package capitec.branch.appointment.otp.app;

import capitec.branch.appointment.exeption.TokenExpiredException;
import capitec.branch.appointment.otp.domain.OTP;
import capitec.branch.appointment.otp.domain.OTPSTATUSENUM;
import capitec.branch.appointment.otp.domain.OTPService;
import capitec.branch.appointment.utils.UseCase;
import capitec.branch.appointment.utils.Username;
import jakarta.ws.rs.InternalServerErrorException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Validated
public class ValidateOTPUseCase implements ValidateOTPService {
    @Value("${otp.number.verification.attempts:2}")
    public  int MAX_NUMBER_OF_VERIFICATION_ATTEMPTS;
    private final OTPService otpService;

    @Override
    public boolean validateOTP(@Username String username, String otp, String traceId) {
        log.info("Validating OTP for user: {}, traceId: {}", username, traceId);
        Optional<OTP> otpEntity;
        try {

            otpEntity = otpService.validateOTP(username, otp, MAX_NUMBER_OF_VERIFICATION_ATTEMPTS);

        } catch (Exception e) {

            log.error("Failed to validate otp, traceId:{}",traceId, e);
            throw new InternalServerErrorException("Failed to validate otp");
        }


        if (otpEntity.isEmpty()) {
            return false;
        }

        var statusEnum = OTPSTATUSENUM.valueOf(otpEntity.get().getStatus().status());
        return switch (statusEnum) {

            case OTPSTATUSENUM.VALIDATED -> true;

            case OTPSTATUSENUM.REVOKED -> {

                log.error("Too many attempt to verify OTP, traceId:{}", traceId);
                throw new ResponseStatusException(HttpStatus.LOCKED, "Too many attempt to verify OTP, please try 5 hours later.");
            }
            case OTPSTATUSENUM.EXPIRED -> {

                log.error("OTP expired , traceId:{}", traceId);

                otpService.renewOTP(otpEntity.get());

                throw new TokenExpiredException("OTP is expired");
            }
            case OTPSTATUSENUM.VERIFIED -> {

                log.error("OTP already verified , traceId:{}", traceId);
                throw new ResponseStatusException(HttpStatus.IM_USED, "OTP is already verified");
            }
            default -> false;
        };
    }
}
