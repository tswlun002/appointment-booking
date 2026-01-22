package capitec.branch.appointment.branch.infrastructure;


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
                (branch_id, branch_business_id, branch_key, slot_duration, utilization_factor,staff_count, day)
            SELECT
                b.id,                                   
                b.business_id,                          
                :day,
                :slotDuration,
                :utilizationFactor,
                :staffCount,
                :day
            FROM branch_lookup b
            ON CONFLICT (branch_id, day)
            DO UPDATE SET
                slot_duration = EXCLUDED.slot_duration,
                utilization_factor = EXCLUDED.utilization_factor
            """)
    int addBranchAppointmentConfigInfo(@Param("branchId") String branchId, @Param("slotDuration") int slotDuration,
                                       @Param("utilizationFactor") double utilizationFactor,@Param("staffCount") int staffCount, @Param("day") LocalDate day);

    @Modifying
    @Transactional
    @Query("""
            WITH branch_lookup AS (
                SELECT id, branch_id AS business_id 
                FROM branch
                WHERE branch_id = :branchId
            )
            INSERT INTO operation_hours_override
                (branch_id, branch_business_id, branch_key, effective_day, open_time,close_time, closed,reason)
            SELECT
                b.id,                                   
                b.business_id,                          
                :effectiveDate,
                :effectiveDate,
                :openTime,
                :closingTime,
                :closed,
                :reason
            FROM branch_lookup b
            ON CONFLICT (branch_id, effective_day)
            DO UPDATE SET
                open_time = EXCLUDED.open_time,
                close_time = EXCLUDED.close_time,
                closed = EXCLUDED.closed,
                reason = EXCLUDED.reason
            """)
    int addBranchOperationHoursOverride(@Param("branchId") String branchId, @Param("effectiveDate") LocalDate effectiveDate,
                                        @Param("openTime") LocalTime openTime, @Param("closingTime") LocalTime closingTime,
                                        @Param("closed") boolean closed, @Param("reason") String reason);


    @Query(value = """
                        SELECT 
                            u.id,
                            u.branch_id,
                            u.open_time,
                            u.close_time,
                            u.created_at,
                            u.last_modified_date,
                            op.branch_id AS op_branch_id,
                            op.open_time AS op_open_time,
                            op.close_time AS op_close_time,
                            op.effective_day AS op_effect_day,
                            op.closed AS op_closed,
                            op.reason AS op_reason,
                            op.created_date AS op_created_date,
                            op.last_modified_date AS last_modified_date,
                            ba.day AS ba_day,
                            ba.slot_duration AS ba_slot_duration,
                            ba.utilization_factor AS ba_utilization_factor,
                            ba.staff_count AS ba_staff_count
                            FROM branch AS u
                            LEFT JOIN operation_hours_override AS op ON op.branch_id=u.id
                            LEFT JOIN branch_appointment_info AS ba  ON ba.branch_id=u.id
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
                op.branch_id AS op_branch_id,
                op.open_time AS op_open_time,
                op.close_time AS op_close_time,
                op.effective_day AS op_effect_day,
                op.closed AS op_closed,
                op.reason AS op_reason,
                ba.day AS ba_day,
                op.created_date AS op_created_date,
                op.last_modified_date AS last_modified_date,
                ba.slot_duration AS ba_slot_duration,
                ba.utilization_factor AS ba_utilization_factor,
                ba.staff_count AS ba_staff_count
                FROM branch AS u
                LEFT JOIN operation_hours_override AS op ON op.branch_id=u.id
                LEFT JOIN branch_appointment_info AS ba  ON ba.branch_id=u.id
                ORDER BY id ASC
                OFFSET :offset LIMIT :limit
            """)
    Collection<BranchEntity> getAllBranch(@Param("offset") int offset,@Param("limit") int limit);
}
