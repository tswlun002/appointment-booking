package capitec.branch.appointment.otp.infrastructure;

import capitec.branch.appointment.otp.domain.OTP;
import capitec.branch.appointment.otp.domain.OTP_PURPOSE_ENUM;
import capitec.branch.appointment.otp.domain.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Value;

@Mapper(componentModel = "spring")
public abstract class OTPMapper {

    @Value("${otp.number.verification.attempts}")
    private int MAX_NUMBER_OF_VERIFICATION_ATTEMPTS;

    // Entity to Domain mapping
    @Mapping(source = "attempts", target = "verificationAttempts")
    @Mapping(source = "purpose", target = "purpose", qualifiedByName = "mapToDomainPurpose")
    @Mapping(source = "status", target = "status")
    @Mapping(expression = "java(calculateExpiryMinutes())", target = "expireDatetime")
    //@Mapping(target = "isExpired", expression = "java(isOtpExpired(otpEntity))")
    public abstract OTP OTPEntityToOTPModel(OTPEntity otpEntity);

    long calculateExpiryMinutes() {
        return MAX_NUMBER_OF_VERIFICATION_ATTEMPTS;
    }

    // Domain to Entity mapping
    @Mapping(source = "verificationAttempts", target = "attempts")
    @Mapping(source = "purpose", target = "purpose", qualifiedByName = "mapToEntityPurpose")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "code", target = "code")
    @Mapping(source = "creationDate", target = "creationDate")
    @Mapping(source = "expiresDate", target = "expiresDate")
    @Mapping(source = "username", target = "username")
    @Mapping(target = "id", ignore = true) // ID is auto-generated
    abstract OTPEntity OTPToOTPEntity(OTP otp);

    @Named("mapToDomainPurpose")
    static OTP_PURPOSE_ENUM mapToDomainPurpose(OTPPurpose otpPurpose) {
        return otpPurpose != null ? OTP_PURPOSE_ENUM.valueOf(otpPurpose.name()) : null;
    }

    @Named("mapToEntityPurpose")
    static OTPPurpose mapToEntityPurpose(OTP_PURPOSE_ENUM purpose) {
        return purpose != null ? new OTPPurpose(purpose.name()) : null;
    }
}