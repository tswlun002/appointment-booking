package capitec.branch.appointment.otp.infrastructure;

import capitec.branch.appointment.otp.domain.OTPStatus;
import capitec.branch.appointment.otp.domain.VerificationAttempts;
import capitec.branch.appointment.utils.Username;
import capitec.branch.appointment.utils.Validator;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Embedded;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
@Table("otp")
public record OTPEntity(
        @Id Long id,
         String code,
        @Column("created_date")
        @DateTimeFormat(pattern = Validator.DATETIME_FORMATTER)
        LocalDateTime creationDate,
        @Column("expire_date")
        @DateTimeFormat(pattern = Validator.DATETIME_FORMATTER)
        LocalDateTime expiresDate,
        @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL)
        OTPPurpose purpose,
        @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL)
        OTPStatus status,
        @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
        VerificationAttempts attempts,
        @Username
        String username
        ) {
}
