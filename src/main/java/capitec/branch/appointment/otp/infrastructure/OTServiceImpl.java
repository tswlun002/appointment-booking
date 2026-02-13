package capitec.branch.appointment.otp.infrastructure;

import capitec.branch.appointment.exeption.OptimisticLockConflictException;
import capitec.branch.appointment.otp.domain.OTP;
import capitec.branch.appointment.otp.domain.OTPService;
import capitec.branch.appointment.otp.domain.OTPStatus;
import jakarta.validation.Valid;
import jakarta.ws.rs.InternalServerErrorException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Validated
public class OTServiceImpl implements OTPService {
    private final OTPRepository otpRepository;
    private final OTPMapper otpMapper;


    @Override
    @Transactional
    public OTP saveOTP( @Valid OTP OTP) {

        if(OTP==null)throw new IllegalArgumentException("OTP cannot be null");

        try {

            OTPEntity newValue = otpMapper.OTPToOTPEntity(OTP);

            int affectedRows = otpRepository.revokeAndInsertNewOtp(newValue.code(), newValue.creationDate(), newValue.expiresDate(),
                    newValue.purpose(), newValue.status(), newValue.verificationAttempts(), newValue.username(),
                    LocalDateTime.now(), newValue.version());
            if(affectedRows==1) {
                log.debug("User had active otp and is revoked for this otp, otp{}, username:{}",newValue.code(),newValue.username());
            }
            return otpMapper.OTPEntityToOTPModel(newValue);


        } catch (Exception e) {
            if(e instanceof OptimisticLockConflictException || (e.getCause() != null && e.getCause() instanceof  OptimisticLockConflictException)){
                log.error("Optimistic lock conflict", e);
                throw new OptimisticLockConflictException(e.getMessage(),e);
            }
            log.error("Failed to save OTP", e);
            throw e;
        }

    }

    private Set<OTPEntity> findOTPEntity(String username) {
        return otpRepository.findOtpByUserId( username);
    }
    private Optional<OTPEntity> findOTPEntity(String username, String otp,OTPStatus status) {
        return otpRepository.findOtpByCodeAndUserId(otp, username, status.getValue());
    }


    @Override
    public Set<OTP> find(String username) {
        return findOTPEntity( username).stream().map(otpMapper::OTPEntityToOTPModel).collect(Collectors.toSet());
    }

    @Override
    public Optional<OTP> find(String username, String otp, OTPStatus status) {
        return findOTPEntity( username,otp,status).map(otpMapper::OTPEntityToOTPModel);
    }

    @Override
    public boolean deleteUserOTP(String username) {
        var isOTPDeleted=false;
        try {
            isOTPDeleted= otpRepository.deleteByUserId( username);
        } catch (Exception e) {
            log.error("Failed to delete all OTP.\n", e);
            throw new InternalServerErrorException("Internal server error");
        }
        return isOTPDeleted;
    }

    @Override
    public Optional<OTP> findLatestOTP(String username,LocalDateTime fromDate) {
        try{
           return  otpRepository.findLatestOTP(username,fromDate).map(otpMapper::OTPEntityToOTPModel);


        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean update(@Valid OTP otp,OTPStatus oldStatus) {

        try {

            var newValue = otpMapper.OTPToOTPEntity(otp);
            var affectedRows = otpRepository.updateOTP(
                    newValue.username(),
                    newValue.code(),
                    oldStatus.getValue(),
                    newValue.status(),
                    newValue.verificationAttempts()

            );
            return affectedRows==1;


        } catch (Exception e) {
            if (e instanceof OptimisticLockConflictException || (e.getCause() != null && e.getCause() instanceof OptimisticLockConflictException)) {
                log.error("Optimistic lock conflict", e);
                throw new OptimisticLockConflictException(e.getMessage(), e);
            }
            log.error("Failed to update OTP", e);
            throw e;

        }
    }
}
