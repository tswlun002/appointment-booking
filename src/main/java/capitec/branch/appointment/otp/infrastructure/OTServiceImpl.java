package capitec.branch.appointment.otp.infrastructure;

import capitec.branch.appointment.otp.domain.OTP;
import capitec.branch.appointment.otp.domain.OTPService;
import jakarta.ws.rs.InternalServerErrorException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OTServiceImpl implements OTPService {
    private final OTPRepository otpRepository;
    private final OTPMapper otpMapper;


    @Transactional
    @Override
    public OTP saveOTP(OTP OTP) {

        if(OTP==null)throw new IllegalArgumentException("OTP cannot be null");

        try {

            OTPEntity newValue = otpMapper.OTPToOTPEntity(OTP);

            int affectedRows = otpRepository.revokeAndInsertNewOtp(newValue.code(), newValue.creationDate(), newValue.expiresDate(),
                    newValue.purpose(), newValue.status(), newValue.verificationAttempts(), newValue.username());
            if(affectedRows>0) return otpMapper.OTPEntityToOTPModel(newValue);
            else return null;

        } catch (Exception e) {
            log.error("Failed to save OTP", e);
            throw new InternalServerErrorException("Internal Server Error");
        }

    }

    private Set<OTPEntity> findOTPEntity(String username) {
        return otpRepository.findOtpByUserId( username);
    }
    private Optional<OTPEntity> findOTPEntity(String username, String otp) {
        return otpRepository.findOtpByCodeAndUserId(otp, username);
    }

    @Transactional
    @Override
    public void verify(String otp, String username) {

        try {

            var affectedRows = otpRepository.verifyOTP(otp,  username)>0;

            if(!affectedRows){

                log.warn("Failed to change OTP  status to VERIFIED");
            }

        }catch (Exception e) {

            log.error("Failed to verify email because failed to save OTP", e);
            throw new InternalServerErrorException("Internal Server Error");
        }
    }

    @Override
    public Set<OTP> find(String username) {
        return findOTPEntity( username).stream().map(otpMapper::OTPEntityToOTPModel).collect(Collectors.toSet());
    }

    @Override
    public Optional<OTP> find(String username, String otp) {
        return findOTPEntity( username,otp).map(otpMapper::OTPEntityToOTPModel);
    }

    @Transactional
    @Override
    public void renewOTP(OTP otpModel) {
        try{

            String code = otpModel.getNewOtp().getCode();
            otpRepository.renewOTP(otpModel.getUsername(), code, LocalDateTime.now().plusMinutes(OTP.EXPIRE_TIME_MIN));
        }
        catch (Exception e) {

            log.error("Failed to update OTP verification verificationAttempts", e);
            throw new InternalServerErrorException("Internal error,failed OTP verification");
        }
    }

    @Transactional
    @Override
    public Optional<OTP> validateOTP(String username, String otpCode, int maxAttempts) {
        Optional<OTPEntity> otpEntity = otpRepository.validateOTP( username, otpCode, maxAttempts);
        return otpEntity.map(otpMapper::OTPEntityToOTPModel);
    }

    @Override
    public boolean deleteAllOTP(String traceId) {
        log.warn("You are about to delete all OTPs, traceId:{}", traceId);
        for (var otp:otpRepository.findAll()) {
            var deleted= deleteUserOTP(otp. username());
            if(!deleted){
                return false;
            }
        }
        log.warn("All OTPs are deleted, traceId:{}", traceId);
        return true;
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
}
