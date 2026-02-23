package capitec.branch.appointment.otp.app;

import capitec.branch.appointment.otp.domain.OTPService;
import capitec.branch.appointment.utils.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class DeleteUserOTPsUseCase {

    private final OTPService otpService;

    public void execute(String username , String traceId ) {
        log.info("Received delete user OTP event, traceId:{}", traceId);


        var allDeleted = otpService.deleteUserOTP(username);

        if (allDeleted) {

            log.info("All user OTP are deleted, traceId:{}", traceId);
        } else {

            log.error("Failed to delete OTP, traceId:{}", traceId);
            throw new ResponseStatusException(HttpStatus.EXPECTATION_FAILED, "Failed to delete user.");
        }
    }
}
