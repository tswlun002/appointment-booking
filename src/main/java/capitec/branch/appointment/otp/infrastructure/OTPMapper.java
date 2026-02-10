package capitec.branch.appointment.otp.infrastructure;

import capitec.branch.appointment.otp.domain.OTP;
import capitec.branch.appointment.otp.domain.OTP_PURPOSE_ENUM;
import capitec.branch.appointment.otp.domain.OTPStatus;
import capitec.branch.appointment.otp.domain.VerificationAttempts;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Value;

@Mapper(componentModel = "spring")
public abstract class OTPMapper {

    @Value("${otp.number.verification.attempts}")
    private int MAX_NUMBER_OF_VERIFICATION_ATTEMPTS;

    // Entity to Domain mapping
    @Mapping(source = "verificationAttempts", target = "verificationAttempts", qualifiedByName = "mapToDomainAttempts")
    @Mapping(source = "purpose", target = "purpose", qualifiedByName = "mapToDomainPurpose")
    @Mapping(source = "status", target = "status", qualifiedByName = "mapToDomainStatus")
    @Mapping(expression = "java(calculateExpiryMinutes())", target = "expireDatetime")
    //@Mapping(target = "isExpired", expression = "java(isOtpExpired(otpEntity))")
    public abstract OTP OTPEntityToOTPModel(OTPEntity otpEntity);

    long calculateExpiryMinutes() {
        return MAX_NUMBER_OF_VERIFICATION_ATTEMPTS;
    }

    // Domain to Entity mapping
    @Mapping(source = "verificationAttempts", target = "verificationAttempts", qualifiedByName = "mapToEntityAttempts")
    @Mapping(source = "purpose", target = "purpose", qualifiedByName = "mapToEntityPurpose")
    @Mapping(source = "status", target = "status", qualifiedByName = "mapToEntityStatus")
    @Mapping(source = "code", target = "code")
    @Mapping(source = "creationDate", target = "creationDate")
    @Mapping(source = "expiresDate", target = "expiresDate")
    @Mapping(source = "username", target = "username")
    @Mapping(target = "id", ignore = true) // ID is auto-generated
    public abstract OTPEntity OTPToOTPEntity(OTP otp);

    // Convert Integer (from entity) to VerificationAttempts (domain)
    @Named("mapToDomainAttempts")
    VerificationAttempts mapToDomainAttempts(Integer attempts) {
        return new VerificationAttempts(
                attempts != null ? attempts : 0,
                MAX_NUMBER_OF_VERIFICATION_ATTEMPTS
        );
    }

    // Convert VerificationAttempts (domain) to Integer (for entity)
    @Named("mapToEntityAttempts")
    static Integer mapToEntityAttempts(VerificationAttempts verificationAttempts) {
        return verificationAttempts != null ? verificationAttempts.attempts() : 0;
    }

    // Convert String (from entity) to OTP_PURPOSE_ENUM (domain)
    @Named("mapToDomainPurpose")
    static OTP_PURPOSE_ENUM mapToDomainPurpose(String purpose) {
        return purpose != null ? OTP_PURPOSE_ENUM.valueOf(purpose) : null;
    }

    // Convert OTP_PURPOSE_ENUM (domain) to String (for entity)
    @Named("mapToEntityPurpose")
    static String mapToEntityPurpose(OTP_PURPOSE_ENUM purpose) {
        return purpose != null ? purpose.name() : null;
    }

    // Convert String (from entity) to OTPStatus (domain)
    @Named("mapToDomainStatus")
    static OTPStatus mapToDomainStatus(String status) {
        return status != null ? new OTPStatus(status) : null;
    }

    // Convert OTPStatus (domain) to String (for entity)
    @Named("mapToEntityStatus")
    static String mapToEntityStatus(OTPStatus status) {
        return status != null ? status.status() : null;
    }
}