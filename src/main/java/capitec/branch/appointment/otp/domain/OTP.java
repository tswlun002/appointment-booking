package capitec.branch.appointment.otp.domain;

import capitec.branch.appointment.sharekernel.username.UsernameGenerator;
import capitec.branch.appointment.utils.OTPCode;
import capitec.branch.appointment.utils.Username;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public class OTP {

    private static final Logger log = LoggerFactory.getLogger(OTP.class);

    public static final int CODE_FIELD_LENGTH = 6;

    private static   ChronoUnit expireTimeUnits = ChronoUnit.MINUTES;

    private  long duration;

    @OTPCode
    private String code;

    @NotNull
    private LocalDateTime creationDate;

    @NotNull
    private LocalDateTime expiresDate;

    @NotNull
    private final OTPPurpose purpose;
    @NotNull
    private VerificationAttempts verificationAttempts;

    @Username
    private final String username;

    private OTPStatus status;

    @Min(0)
    private int version;
    public OTP(String username, LocalDateTime expireDatetime, @NotNull OTPPurpose purpose, @NotNull VerificationAttempts verificationAttempts) {

        this(username, expireDatetime, purpose, verificationAttempts, null);
    }
    public OTP(String username, LocalDateTime expireDatetime, @NotNull OTPPurpose purpose, @NotNull VerificationAttempts verificationAttempts,ChronoUnit TimeUnits) {

        expireTimeUnits = TimeUnits==null?expireTimeUnits: TimeUnits;


        Assert.isTrue(UsernameGenerator.isValid(username), "Username is invalid");
        Assert.notNull(expireDatetime, "Expire date cannot be null");
        LocalDateTime now = LocalDateTime.now();
        duration = calculate(now,expireDatetime,expireTimeUnits);
        Assert.isTrue(duration > 0, "Expire date must be after creation date.");
        Assert.notNull(purpose, "Purpose cannot be null");
        Assert.notNull(verificationAttempts, "Verification attempts cannot be null");
        Assert.notNull(purpose, "Purpose cannot be null");
        Assert.isTrue(version >= 0, "Existing OTP version must be greater than 0");

        this.username = username;
        this.creationDate = now;
        this.expiresDate = expireDatetime;
        this.verificationAttempts = verificationAttempts;
        this.purpose = purpose;
        this.status = OTPStatus.CREATED;
        this.code = SecurePassword.generatePassword(CODE_FIELD_LENGTH);
        this.version = 0;

    }

    private OTP(String username, String code, LocalDateTime creationDate, LocalDateTime expiresDate,
                OTPPurpose purpose, VerificationAttempts verificationAttempts, OTPStatus status, long duration, int version) {

        this.username = username;
        this.duration = duration;
        this.code = code;
        this.creationDate = creationDate;
        this.expiresDate = expiresDate;
        this.purpose = purpose;
        this.verificationAttempts = verificationAttempts;
        this.status = status;
        this.version = version;
    }

    public static OTP creatFromExisting(String username, String code, LocalDateTime creationDate, LocalDateTime expiresDate,
                                        OTPPurpose purpose, VerificationAttempts verificationAttempts, OTPStatus status,
                                        int version, ChronoUnit units) {


        expireTimeUnits = units ==null? expireTimeUnits : units;
        Assert.isTrue(UsernameGenerator.isValid(username), "Username is invalid");
        Assert.notNull(creationDate, "Creation Date cannot be null");
        Assert.notNull(expiresDate, "Expires Date cannot be null");
        long periodMinutes = calculate(creationDate, expiresDate, expireTimeUnits);
        Assert.isTrue(periodMinutes > 0, "Expire date must be after creation date.");
        Assert.notNull(purpose, "Purpose cannot be null");
        Assert.notNull(verificationAttempts, "Verification attempts cannot be null");
        Assert.notNull(purpose, "Purpose cannot be null");
        Assert.isTrue(version >= 0, "Existing OTP version must be greater or equal 0");

        return new OTP(username, code, creationDate, expiresDate, purpose, verificationAttempts, status, periodMinutes, version);
    }


    private static long calculate(LocalDateTime creationDate, LocalDateTime expiresDate, ChronoUnit units) {
       return Duration.between(creationDate, expiresDate).get(units);
    }
    public void renewOTP() {

        if (this.status != OTPStatus.EXPIRED) {
            log.debug("Cannot renew an OTP that is not at expired state, OTP: {}", this);
            throw new IllegalStateException("Only expired OTPs can be renewed");
        }

        log.debug("Renewing an OTP that is at expired state, previous state OTP: {}", this);
        this.expiresDate = LocalDateTime.now().plus(duration, this.getExpireTimeUnits());
        this.code = SecurePassword.generatePassword(CODE_FIELD_LENGTH);
        this.status = OTPStatus.RENEWED;
        this.creationDate = LocalDateTime.now();
        this.version = 0;
    }

    public void revoke() {

        if (this.status == OTPStatus.VALIDATED || this.status == OTPStatus.VERIFIED) {
            log.debug("Cannot revoke an  verified/validated OTP, OTP: {}", this);
            throw new IllegalStateException("Cannot revoke an verified OTP");
        }

        if (this.status == OTPStatus.REVOKED) {
            log.debug("Cannot revoke an OTP that is already revoked, OTP: {}", this);
            throw new IllegalStateException("Cannot revoke an revoked OTP");
        }
        log.debug("Revoking an OTP. Previous state OTP: {}", this);
        this.status = OTPStatus.REVOKED;
        //  this.version +=1;  infrastructure will take care, uncomment is user db does not manage optimistic lock

    }

    public void validate(String code) {

        if (this.status == OTPStatus.VERIFIED) {
            throw new IllegalStateException("OTP is already verified");
        }
        if (this.status == OTPStatus.VALIDATED) {
            throw new IllegalStateException("OTP is already validated");
        }
        if (this.status == OTPStatus.REVOKED) {
            throw new IllegalStateException("OTP is revoked");
        }

        if (this.status == OTPStatus.CREATED || this.status == OTPStatus.RENEWED) {

            int attempts = this.verificationAttempts.attempts();
            int maxNumberVerificationAttempts = this.verificationAttempts.maxNumberVerificationAttempts();
            this.verificationAttempts = new VerificationAttempts(attempts + 1, maxNumberVerificationAttempts);
            //  2this.version +=1;  infrastructure will take care, uncomment is user db does not manage optimistic lock
            if (this.expiresDate.isBefore(LocalDateTime.now())) {
                this.status = OTPStatus.EXPIRED;
                return;
            } else if (this.verificationAttempts.usedAllAttempts() && !Objects.equals(code, this.code)) {
                this.status = OTPStatus.REVOKED;
                return;
            }
            if (Objects.equals(code, this.code)) {
                this.status = OTPStatus.VALIDATED;
                return;
            }
        }
    }

    public ChronoUnit getExpireTimeUnits() {
        return expireTimeUnits;
    }

    public void setExpireTimeUnits(ChronoUnit units) {
        if (units == null) {
           log.info("expireTimeUnits set to default units because expireTimeUnits is null");
           expireTimeUnits = ChronoUnit.MINUTES;
           return;
        }
        expireTimeUnits = units;
        this.duration = calculate(this.creationDate, this.expiresDate, expireTimeUnits);

    }


    public long getDuration() {
        return duration;
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

    public OTPPurpose getPurpose() {
        return purpose;
    }

    public VerificationAttempts getVerificationAttempts() {
        return verificationAttempts;
    }

    public String getUsername() {
        return username;
    }

    public OTPStatus getStatus() {
        return status;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof OTP otp)) return false;
        return Objects.equals(code, otp.code) && Objects.equals(username, otp.username) && status == otp.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, username, status);
    }

    @Override
    public String toString() {
        return "OTP{" +
                "periodMinutes=" + duration +
                ", code='" + code + '\'' +
                ", creationDate=" + creationDate +
                ", expiresDate=" + expiresDate +
                ", purpose=" + purpose +
                ", verificationAttempts=" + verificationAttempts +
                ", username='" + username + '\'' +
                ", status=" + status +
                ", version=" + version +
                '}';
    }
}
