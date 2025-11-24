package lunga.appointmentbooking.otp.domain;

import lunga.appointmentbooking.utils.OTPCode;
import lunga.appointmentbooking.utils.Username;
import lunga.appointmentbooking.utils.Validator;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.InternalServerErrorException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.Range;

import java.time.LocalDateTime;

@Slf4j
public class OTP {

    public static final int CODE_FIELD_LENGTH = 6;
    public  static  final int EXPIRE_TIME_MIN=3;

    @Range(min = EXPIRE_TIME_MIN, message = Validator.OTP_EXPIRE_TIME_MESS)
    private  final long expireDatetime;

    @OTPCode
    private String code ;

    private  LocalDateTime creationDate ;

    private LocalDateTime expiresDate ;

    @NonNull
    private OTP_PURPOSE_ENUM purpose;

    @NonNull
    private  VerificationAttempts verificationAttempts;

     @Username
    private final String username;

    private OTPStatus status;

    public OTP(String username, long expireDatetime, @NotNull OTP_PURPOSE_ENUM purpose, @NonNull VerificationAttempts verificationAttempts) {
        this. username =  username;
        this.expireDatetime = expireDatetime;
        this.creationDate = LocalDateTime.now();
        this.expiresDate = creationDate.plusMinutes(expireDatetime);
        this.verificationAttempts = verificationAttempts;
        this.purpose = purpose;
        this.status=new OTPStatus(OTPSTATUSENUM.CREATED.name());
        this.code= SecurePassword.generatePassword(CODE_FIELD_LENGTH);
        validate();
    }

    public final OTP getNewOtp(){
        this.expiresDate = LocalDateTime.now().plusMinutes(expireDatetime);
        this.code = SecurePassword.generatePassword(CODE_FIELD_LENGTH);
        this.status =  new OTPStatus(OTPSTATUSENUM.CREATED.name());
        this.creationDate= LocalDateTime.now();
        validate();
        return this;
    }

    public void setPurpose(@NonNull OTP_PURPOSE_ENUM purpose) {
        this.purpose = purpose;
    }

    public  final  OTP  validate(){
        if(creationDate.equals(expiresDate)||creationDate.isAfter(expiresDate)) {
            log.error("Invalid creation date, it cannot be same or after expire datetime, creation date: {}, expiresDate: {}", creationDate, expiresDate);
            throw  new InternalServerErrorException("Internal Server Error");
        }
        Validator.validate(this);
        return this;
    }
    public   void  setVerificationAttempts(@NonNull VerificationAttempts attempts) {
        this.verificationAttempts = attempts;
        validate();
    }

    public void  setStatus(OTPStatus status){
        this.status = status;
    }


    public void setCode( @OTPCode String code) {
        this.code = code;
        validate();
    }

    public  void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
        this.expiresDate = creationDate.plusMinutes(expireDatetime);
        validate();
    }

    public long getExpireDatetime() {
        return expireDatetime;
    }

    public String getCode() {
        return code;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public LocalDateTime getExpiresDate() {
        return expiresDate;
    }


    public @NonNull OTP_PURPOSE_ENUM getPurpose() {
        return purpose;
    }

    public @NonNull VerificationAttempts getVerificationAttempts() {
        return verificationAttempts;
    }

    public String getUsername() {
        return  username;
    }

    public OTPStatus getStatus() {
        return status;
    }
}
