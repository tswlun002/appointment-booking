package capitec.branch.appointment.otp.domain;

import jakarta.ws.rs.InternalServerErrorException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;

@Slf4j
public record VerificationAttempts(
        @Column("verification_attempts")
        int attempts,
        @Value("${otp.number.verification.attempts}")
        @Transient
        int maxNumberVerificationAttempts
) {
    public  VerificationAttempts {
        if (!(0<=attempts && attempts<=maxNumberVerificationAttempts) ) {
            log.error("Verification attempts:{} exceeded maximum attempts:{} of OTP verification attempts", attempts,maxNumberVerificationAttempts);
            throw new InternalServerErrorException("Verification attempts exceeded maximum attempts of verification attempts");
        }
    }
}
