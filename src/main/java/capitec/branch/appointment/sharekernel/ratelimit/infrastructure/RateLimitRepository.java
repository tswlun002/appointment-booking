package capitec.branch.appointment.sharekernel.ratelimit.infrastructure;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RateLimitRepository extends CrudRepository<RateLimitEntity, Long> {

    @Query("SELECT * FROM rate_limit WHERE identifier = :identifier AND purpose = :purpose")
    Optional<RateLimitEntity> findByIdentifierAndPurpose(@Param("identifier") String identifier,
                                                          @Param("purpose") String purpose);

    @Modifying
    @Query("DELETE FROM rate_limit WHERE identifier = :identifier AND purpose = :purpose")
    void deleteByIdentifierAndPurpose(@Param("identifier") String identifier,
                                       @Param("purpose") String purpose);

    @Modifying
    @Query("DELETE FROM rate_limit WHERE window_start_at < :cutoffTime")
    void deleteExpiredEntries(@Param("cutoffTime") LocalDateTime cutoffTime);
}
