package capitec.branch.appointment.branch.infrastructure;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.InsertOnlyProperty;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Table("operation_hours_override")
public record OperationHoursOverrideEntity(
        @Column("branch_business_id")
        String branchId,
        @Column("effective_date")
        LocalDate effectiveDate,
        @Column("open_at")
        LocalTime openAt,
        @Column("close_at")
        LocalTime closeAt,
        @Column("closed")
        boolean closed,
        @Column("reason")
        String reason,
        @CreatedDate
        @InsertOnlyProperty
        @Column("created_date")
        LocalDateTime createdDate,
        @LastModifiedDate
        @Column("last_modified_date")
        LocalDateTime last_modified_date

){

}