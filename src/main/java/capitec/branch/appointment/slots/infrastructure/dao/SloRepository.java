package capitec.branch.appointment.slots.infrastructure.dao;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
interface SloRepository extends CrudRepository<SlotEntity, UUID> {
    @Query(value="""
            SELECT
            id, day, start_time, end_time,
            max_booking_capacity,booking_count, branch_id, status, 
            created_at, last_modified_date, version
            FROM slot 
            WHERE slot.branch_id=:branchId AND slot.day=:day
            """)
    List<SlotEntity> dailySlot(@Param("branchId")String branchId,@Param("day") LocalDate day);
    @Query(value = """
                SELECT 
                id, day, start_time, end_time,
                max_booking_capacity,booking_count, branch_id, status, 
                created_at, last_modified_date, version
                FROM slot 
                WHERE  slot.branch_id=:branchId AND slot.day >= :date AND (CAST(:status AS VARCHAR) IS NULL OR slot.status = :status)
            """)
    List<SlotEntity> nextDaySlots(@Param("branchId")String branchId, @Param("date") LocalDate date, @Param("status") String status);

    @Modifying
    @Query("""
            DELETE  FROM  slot AS s WHERE  s.id=:id  
            """)
    int deleteSlotEntitiesBySlotId(@Param("id") UUID id);
     @Query("""
        SELECT  day FROM slot WHERE day >=:fromDate ORDER BY day DESC LIMIT 1
        """)
     Optional<LocalDate> getLastestGeneratedSlotDate(@Param("fromDate") LocalDate fromDate);
}
