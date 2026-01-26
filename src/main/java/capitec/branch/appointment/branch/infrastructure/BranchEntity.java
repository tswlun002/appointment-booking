package capitec.branch.appointment.branch.infrastructure;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Table("branch")
 record BranchEntity(
        @Id
        Long id,
        @Column("branch_id")
        String branchId,
        @Column("branch_name")
        String branchName,
        @MappedCollection(idColumn = "branch_id", keyColumn = "branch_key")
        Map<LocalDate, BranchAppointmentInfoEntity> branchAppointmentInfo,
        @MappedCollection(idColumn = "branch_id", keyColumn = "branch_key")
        Map<LocalDate,OperationHoursOverrideEntity> operationHoursOverride,
        @CreatedDate
        @Column("created_at")
        LocalDateTime createdAt,
        @LastModifiedDate
        @Column("last_modified_date")
        LocalDateTime updatedAt
        ) {
}
