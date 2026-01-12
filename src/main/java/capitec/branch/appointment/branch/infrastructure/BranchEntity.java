package capitec.branch.appointment.branch.infrastructure;

import capitec.branch.appointment.branch.domain.appointmentinfo.DayType;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.InsertOnlyProperty;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

@Table("branch")
 record BranchEntity(
        @Id
        Long id,
        @Column("branch_id")
        String branchId,
        @Column("open_time")
        LocalTime openTime,
        @Column("close_time")
        LocalTime closingTime,
        @MappedCollection(idColumn = "branch_id", keyColumn = "day_type")
        Map<DayType, BranchAppointmentInfoEntity> branchAppointmentInfo,
        @MappedCollection(idColumn = "branch_id")
        AddressEntity address,
        @Column("created_at")
        @InsertOnlyProperty
        LocalDateTime createdAt,
        @Column("last_modified_date")
        LocalDateTime updatedAt
        ) {
}
