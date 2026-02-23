package capitec.branch.appointment.otp.infrastructure;

import capitec.branch.appointment.utils.Username;
import capitec.branch.appointment.utils.ValidatorMessages;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
@Table("otp")
record OTPEntity(
        @Id Long id,
         String code,
        @Column("created_date")
        @DateTimeFormat(pattern = ValidatorMessages.DATETIME_FORMATTER)
        LocalDateTime creationDate,
        @Column("expire_date")
        @DateTimeFormat(pattern = ValidatorMessages.DATETIME_FORMATTER)
        LocalDateTime expiresDate,
        @Column("purpose")
        String purpose,
        String status,
        @Column("verification_attempts")
        Integer verificationAttempts,
        @Username
        String username,
        @Column("updated_at")
        @LastModifiedBy
        LocalDateTime updatedAt,
        @Version
        int version
        ) {

}
