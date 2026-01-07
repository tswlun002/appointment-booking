package capitec.branch.appointment.slots.infrastructure.dao;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.time.LocalDate;
import java.util.List;

@Repository
interface SloRepository extends CrudRepository<SlotEntity, String> {
    @Query(value="""
            SELECT
            id, day, start_time, end_time,
            number, branch_id, status, 
            created_at, last_modified_date, version
            FROM slot 
            WHERE slot.branch_id=:branchId AND slot.day=:day
            """)
    List<SlotEntity> dailySlot(@Param("branchId")String branchId,@Param("day") LocalDate day);
    @Query(value = """
                SELECT 
                id, day, start_time, end_time,
                number, branch_id, status, 
                created_at, last_modified_date, version
                FROM slot 
                WHERE  slot.branch_id=:branchId AND slot.day >= :date AND (:status IS NULL OR slot.status = :status)
            """)
    List<SlotEntity> next7DaySlots(@Param("branchId")String branchId,@Param("date") LocalDate date, @Param("status") String status);

    @Modifying
    @Query("""
            DELETE  FROM  slot AS s WHERE  s.id=:id  
            """)
    int deleteSlotEntitiesBySlotId(@Param("id") String id);

}
