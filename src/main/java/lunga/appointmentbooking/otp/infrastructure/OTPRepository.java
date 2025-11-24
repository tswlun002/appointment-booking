package lunga.appointmentbooking.otp.infrastructure;


import jakarta.validation.constraints.NotBlank;
import lunga.appointmentbooking.utils.Username;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

public interface OTPRepository extends CrudRepository<OTPEntity, Long> {


    @Modifying
    @Query(value = """
                WITH revoked_otps AS (
                    UPDATE otp SET status = 'REVOKED'
                    WHERE  username = :username AND status IN ('RENEWED', 'CREATED') 
                    RETURNING  username 
                )
                INSERT INTO otp (code, created_date, expire_date, purpose, status, verification_attempts, username) 
                VALUES( :code, :creationDate, :expiresDate, :purpose, :status, :attempts, :username )
            """
           )
    int  revokeAndInsertNewOtp(@Param("code") String code, @Param("creationDate") LocalDateTime creationDate,
                               @Param("expiresDate") LocalDateTime expiresDate,
                                @Param("purpose") String purpose,@Param("status") String status,@Param("attempts") int attempts,
                               @Param("username") String username);
    @Query("""
               SELECT otp.id, otp.code,otp.purpose, otp.status ,otp.created_date,otp.expire_date , otp. username, otp.verification_attempts    FROM  otp as otp
               WHERE otp.code=:code AND otp. username=:username
            """)
    Optional<OTPEntity> findOtpByCodeAndUserId(@Param("code") String code, @Param("username") String username);

    @Query("""
               SELECT otp.id, otp.code,otp.purpose, otp.status ,otp.created_date,otp.expire_date , otp. username ,otp.verification_attempts FROM  otp as otp
               WHERE  otp. username=:username
            """)
    Set<OTPEntity> findOtpByUserId(@Username String username);

    @Modifying
    @Query("""
            DELETE FROM otp as otp WHERE otp. username=:username
            """)
    boolean deleteByUserId(@NotBlank @Param("username") String username);

   // @Modifying
   @Query("""
    UPDATE otp SET
        verification_attempts = CASE
           WHEN otp.status IN  ('VERIFIED','REVOKED','VALIDATED') THEN otp.verification_attempts
           ELSE otp.verification_attempts + 1
       END,  
        status = CASE
            WHEN otp.status IN  ('VERIFIED','REVOKED','VALIDATED')  THEN otp.status
            WHEN otp.expire_date <= LOCALTIMESTAMP THEN 'EXPIRED'                    -- Check expiry FIRST
            WHEN otp.verification_attempts+1>=:maxAttempts AND otp.code!=:code AND otp. username=:username THEN 'REVOKED'
            WHEN otp.code=:code AND otp. username=:username THEN 'VALIDATED'                    -- Check code match AFTER expiry
            ELSE otp.status
        END 
    WHERE otp. username=:username AND otp.status in ('CREATED','RENEWED')
    RETURNING otp.id, otp.code,otp.purpose, otp.status ,otp.created_date,otp.expire_date , otp. username ,otp.verification_attempts
    """)
    Optional<OTPEntity> validateOTP( @Param("username") String username,@Param("code") String code, @Param("maxAttempts") int maxAttempts);

    @Modifying
    @Query("""
        UPDATE otp o SET code=:newCode, expire_date=:expirationTime, status='RENEWED'
        WHERE o. username=:username AND o.status='EXPIRED'
    """)
    boolean renewOTP(@Param("username") String username,@Param("newCode") String newCode, @Param("expirationTime") LocalDateTime expirationTime);

    @Modifying
    @Query("""
        UPDATE otp SET  status='VERIFIED' WHERE code=:code AND  username=:username AND status='VALIDATED'
        """)
    int verifyOTP(String code, String username);



  /*  @Query("""
    SELECT  LOCALTIMESTAMP as time_now, otp.expire_date,
            (LOCALTIMESTAMP>= otp.expire_date) as is_expired
    FROM otp 
    WHERE otp. username = :username
    """)
    List<otp> isOTPExpired(@Param("username") String username);*/
}

//record otp(Object time_now, Object expire_date, Object is_expired) {}