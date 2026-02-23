package capitec.branch.appointment.otp.domain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
public record VerificationAttempts(
        int attempts,
        @Value("${otp.number.verification.attempts}")
        int maxNumberVerificationAttempts
) {
    public  VerificationAttempts {
        if (!(0<=attempts && attempts<=maxNumberVerificationAttempts) ) {
            log.error("Verification attempts:{} exceeded maximum attempts:{} of OTP verification attempts", attempts,maxNumberVerificationAttempts);
            throw new IllegalStateException("Exceeded maximum verification attempts");
        }
    }
    boolean usedAllAttempts(){
        return attempts >= maxNumberVerificationAttempts;
    }
}
