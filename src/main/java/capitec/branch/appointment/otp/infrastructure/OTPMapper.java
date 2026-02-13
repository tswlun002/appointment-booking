package capitec.branch.appointment.otp.infrastructure;

import capitec.branch.appointment.otp.domain.OTP;
import capitec.branch.appointment.otp.domain.OTPPurpose;
import capitec.branch.appointment.otp.domain.OTPStatus;
import capitec.branch.appointment.otp.domain.VerificationAttempts;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Value;

import java.time.temporal.ChronoUnit;

@Mapper(componentModel = "spring")
abstract class OTPMapper {

    @Value("${otp.number.verification.attempts}")
    private int MAX_NUMBER_OF_VERIFICATION_ATTEMPTS;
    @Value("${otp.expire.chron-units}")
    private ChronoUnit CHRON_UNITS;

    OTP OTPEntityToOTPModel(OTPEntity otpEntity){
        return  OTP.creatFromExisting(
                otpEntity.username(),
                otpEntity.code(),
                otpEntity.creationDate(),
                otpEntity.expiresDate(),
                mapToDomainPurpose(otpEntity.purpose()),
                mapToDomainAttempts(otpEntity.verificationAttempts()),
                mapToDomainStatus(otpEntity),
                otpEntity.version(),
                CHRON_UNITS
        );
    }


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
    @Mapping(source = "version", target = "version")
    @Mapping(target = "id", ignore = true)
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
    static OTPPurpose mapToDomainPurpose(String purpose) {
        return purpose != null ? OTPPurpose.valueOf(purpose) : null;
    }

    // Convert OTP_PURPOSE_ENUM (domain) to String (for entity)
    @Named("mapToEntityPurpose")
     String mapToEntityPurpose(OTPPurpose otpPurpose) {
        return otpPurpose != null ? otpPurpose.name() : null;
    }

    // Convert String (from entity) to OTPStatus (domain)
    @Named("mapToDomainStatus")
     OTPStatus mapToDomainStatus(OTPEntity otpEntity) {
        return otpEntity.status() != null ? OTPStatus.valueOf(otpEntity.status()) : null;
    }

    // Convert OTPStatus (domain) to String (for entity)
    @Named("mapToEntityStatus")
     String mapToEntityStatus(OTPStatus status) {
        return status != null ? status.getValue() : null;
    }
}