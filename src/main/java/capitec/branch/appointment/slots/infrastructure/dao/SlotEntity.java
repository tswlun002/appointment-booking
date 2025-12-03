package capitec.branch.appointment.slots.infrastructure.dao;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Table("slot")
public record SlotEntity(
        @Id Long id,
        LocalDate day,
        @Column("start_time")
        LocalTime startTime,
        @Column("end_time")
        LocalTime endTime,
        Integer number,
        @Column("is_booked")
        boolean isBooked,
        @Column("created_at")
        LocalDateTime createdAt,
        @Column("last_modified_date")
        LocalDateTime updatedAt,
        @Version
        int version

) {
}
