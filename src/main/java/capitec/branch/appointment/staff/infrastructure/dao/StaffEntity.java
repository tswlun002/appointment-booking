package capitec.branch.appointment.staff.infrastructure.dao;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;

@Table("staff")
record StaffEntity(
        @Id
        String id,
        String username,
        String status,
        @Column("branch_id")
        String branchId,
        @Column("created_at")
        LocalDateTime createdAt,
        @Column("last_modified_date")
        LocalDateTime updatedAt
) {
}
