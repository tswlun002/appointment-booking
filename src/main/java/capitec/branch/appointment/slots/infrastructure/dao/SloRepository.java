package capitec.branch.appointment.slots.infrastructure.dao;

import capitec.branch.appointment.slots.domain.Slot;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.time.LocalDate;
import java.util.List;

@Repository
interface SloRepository extends CrudRepository<SlotEntity, Long> {
    @Query(value="""
                    SELECT id, day, start_time, end_time,number,is_booked,created_at,last_modified_date,version FROM slot 
                    WHERE slot.day=:day
                """)
    List<Slot> dailySlot(@Param("day") LocalDate day);
    @Query(value = """
                SELECT * FROM slot  WHERE slot.day >= :date AND (:status IS NULL OR slot.is_booked = :status)
        """)
    List<Slot> next7DaySlots(@Param("date") LocalDate date, @Param("status") Boolean status);

    @Modifying
    @Query("""
            DELETE  FROM  slot AS s WHERE  s.number=:number
            """)
    int deleteSlotEntitiesByNumber(@Param("number") int number);
}
