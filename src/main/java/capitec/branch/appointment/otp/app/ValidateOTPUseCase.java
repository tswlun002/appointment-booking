package capitec.branch.appointment.otp.app;

import capitec.branch.appointment.exeption.OTPExpiredException;
import capitec.branch.appointment.exeption.OptimisticLockConflictException;
import capitec.branch.appointment.otp.domain.OTP;
import capitec.branch.appointment.otp.domain.OTPService;
import capitec.branch.appointment.otp.domain.OTPStatus;
import capitec.branch.appointment.utils.UseCase;
import capitec.branch.appointment.utils.Username;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Validated
public class ValidateOTPUseCase implements ValidateOTPService {

    private final OTPService otpService;

    @Override
    public boolean validateOTP(@Username String username, String otp, String traceId) {

        log.info("Validating OTP for user: {}, traceId: {}", username, traceId);
        Optional<OTP> optionalOTP = otpService.findLatestOTP(username, LocalDateTime.now().minusMonths(30));
        try {

            if (optionalOTP.isPresent()) {


                OTP otpObj = optionalOTP.get();

                if (otpObj.getStatus() == OTPStatus.VERIFIED || otpObj.getStatus() == OTPStatus.VALIDATED) {

                    log.info("OTP is already verified");
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP is already verified");
                }

                if (otpObj.getStatus() == OTPStatus.REVOKED) {

                    log.info("OTP is locked. Too many attempt to verify OTP, traceId:{}", traceId);
                    throw new ResponseStatusException(HttpStatus.LOCKED, "Too many attempt to verify OTP, please try 5 hours later.");
                }
                 var oldStatus = otpObj.getStatus();
                //validate
                otpObj.validate(otp);

                return switch (otpObj.getStatus()) {

                    case VALIDATED -> {
                        //persist change
                        otpService.update(otpObj,oldStatus);
                        yield true;
                    }
                    case REVOKED -> {
                        //persist change
                        otpService.update(otpObj,oldStatus);

                        log.info("OTP is locked. Too many attempt to verify OTP, traceId:{}", traceId);
                        throw new ResponseStatusException(HttpStatus.LOCKED, "Too many attempt to verify OTP, please try 5 hours later.");
                    }
                    case EXPIRED -> {

                        //persist change
                         otpObj.renewOTP();

                        otpService.saveOTP(otpObj);
                        log.error("OTP expired , traceId:{}", traceId);

                        throw new OTPExpiredException("OTP is expired");
                    }
                    default -> {
                        //persist change
                        otpService.update(otpObj, oldStatus);
                       yield  false;
                    }
                };
            } else
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You have no valid OTP in past month upto now");
        }
        catch (OTPExpiredException ex) {
            throw ex;
        }
        catch (ResponseStatusException e) {
            log.info("Updated OTP response, traceId:{}", traceId,e);
            throw e;
        }
        catch (IllegalArgumentException | IllegalStateException e) {

            log.info("Invalid OTP variables , traceId:{}", traceId, e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(),e);
        }
        catch (OptimisticLockConflictException e) {
            log.error("OPT is being updated multiple processes at same time, traceId:{}", traceId, e);

            throw new ResponseStatusException(HttpStatus.CONFLICT, "OPT is being updated multiple processes at same time. Please try again",e);
        }
        catch (Exception e) {
            log.error("OTP validation failed, traceId:{}", traceId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to validate OTP. Please try again",e);
        }
    }
}
