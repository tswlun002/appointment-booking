package capitec.branch.appointment.slots.infrastructure.dao;

import org.springframework.data.annotation.*;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Table("slot")
public record SlotEntity(
        @Id String id,
        LocalDate day,
        @Column("start_time") LocalTime startTime,
        @Column("end_time") LocalTime endTime,
        Integer number,
        @Column("branch_id") String branchId,
        String status,
        @CreatedDate @Column("created_at") LocalDateTime createdAt,
        @LastModifiedDate @Column("last_modified_date") LocalDateTime updatedAt,
        @Version int version
) {
    // Mark canonical constructor for Spring Data JDBC
    @PersistenceCreator
    public SlotEntity {
    }

    public SlotEntity(UUID id, LocalDate day, LocalTime startTime, LocalTime endTime,
                      Integer number, String branchId, String status) {
        this(id.toString(), day, startTime, endTime, number, branchId, status, null, null, 0);
    }

    public SlotEntity(UUID id, SlotEntity entity) {
        this(id.toString(), entity.day, entity.startTime, entity.endTime,
                entity.number, entity.branchId, entity.status,
                entity.createdAt, entity.updatedAt, entity.version);
    }

    public SlotEntity withVersion(int version) {
        return new SlotEntity(this.id, this.day, this.startTime, this.endTime,
                this.number, this.branchId, this.status,
                this.createdAt, this.updatedAt, version);
    }
}