package capitec.branch.appointment.branch.infrastructure;

import capitec.branch.appointment.day.domain.DayType;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.InsertOnlyProperty;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.Set;

@Table("branch")
 record BranchEntity(
        @Id
        Long id,
        @Column("branch_id")
        String branchId,
        @Column("open_time")
        LocalTime openTime,
        @Column("closing_time")
        LocalTime closingTime,
        @MappedCollection(idColumn = "branch_id", keyColumn = "day_type")
        Map<DayType, BranchAppointmentInfoEntity> branchAppointmentInfo,
        @MappedCollection(idColumn = "branch_id")
        Set<BranchStaffAssignmentEntity>dailyStaff,
        @MappedCollection(idColumn = "branch_id")
        AddressEntity address,
        @Column("created_at")
        @InsertOnlyProperty
        LocalDateTime createdAt,
        @Column("last_modified_date")
        LocalDateTime updatedAt
        ) {
}
