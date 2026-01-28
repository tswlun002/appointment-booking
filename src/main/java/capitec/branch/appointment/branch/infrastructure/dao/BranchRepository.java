package capitec.branch.appointment.branch.infrastructure.dao;


import capitec.branch.appointment.branch.domain.appointmentinfo.DayType;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Optional;

@Repository
interface BranchRepository extends CrudRepository<BranchEntity, Long> {
    @Modifying
    @Transactional
    @Query("""
            WITH branch_lookup AS (
                SELECT id, branch_id AS business_id 
                FROM branch
                WHERE branch_id = :branchId
            )
            INSERT INTO branch_appointment_info
                (branch_id, branch_business_id, branch_key, slot_duration, utilization_factor,staff_count, day,max_booking_capacity)
            SELECT
                b.id,                                   
                b.business_id,                          
                :day,
                :slotDuration,
                :utilizationFactor,
                :staffCount,
                :day,
                :maxBookingCapacity
                
            FROM branch_lookup b
            ON CONFLICT (branch_id, day)
            DO UPDATE SET
                slot_duration = EXCLUDED.slot_duration,
                utilization_factor = EXCLUDED.utilization_factor,
                max_booking_capacity= EXCLUDED.max_booking_capacity
            """)
    int addBranchAppointmentConfigInfo(@Param("branchId") String branchId, @Param("slotDuration") int slotDuration,
                                       @Param("utilizationFactor") double utilizationFactor, @Param("staffCount") int staffCount,
                                       @Param("day") DayType day, @Param("maxBookingCapacity")  int maxBookingCapacity);

    @Modifying
    @Transactional
    @Query("""
            WITH branch_lookup AS (
                SELECT id, branch_id AS business_id ,branch_name
                FROM branch
                WHERE branch_id = :branchId
            )
            INSERT INTO operation_hours_override
                (branch_id, branch_business_id, branch_key, effective_date, open_at,close_at, closed,reason)
            SELECT
                b.id,                                   
                b.business_id,                          
                :effectiveDate,
                :effectiveDate,
                :openAt,
                :closeAt,
                :closed,
                :reason
            FROM branch_lookup b
            ON CONFLICT (branch_id, effective_date)
            DO UPDATE SET
                open_at = EXCLUDED.open_at,
                close_at = EXCLUDED.close_at,
                closed = EXCLUDED.closed,
                reason = EXCLUDED.reason
            """)
    int addBranchOperationHoursOverride(@Param("branchId") String branchId, @Param("effectiveDate") LocalDate effectiveDate,
                                        @Param("openAt") LocalTime openAt, @Param("closeAt") LocalTime closeAt,
                                        @Param("closed") boolean closed, @Param("reason") String reason);


    @Query(value = """
                        SELECT 
                            u.id,
                            u.branch_id,
                            u.branch_name,
                            u.created_at,
                            u.last_modified_date
                            FROM branch AS u
                       WHERE u.branch_id=:branchId 
            """)
    Optional<BranchEntity> getByBranchId(@Param("branchId") String branchId);

    @Modifying
    @Transactional
    @Query("""
            DELETE  FROM branch AS b WHERE  b.branch_id=:branchId
           """)
    int deletBranch(@Param("branchId") String branchId);

    @Query("""
            SELECT 
                u.id,
                u.branch_id,
                u.branch_name,
                u.created_at,
                u.last_modified_date
                FROM branch AS u
                ORDER BY id ASC
                OFFSET :offset LIMIT :limit
            """)
    Collection<BranchEntity> getAllBranch(@Param("offset") int offset,@Param("limit") int limit);
}
