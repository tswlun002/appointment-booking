package capitec.branch.appointment.branch.infrastructure;

import capitec.branch.appointment.staff.domain.Staff;
import capitec.branch.appointment.branch.domain.address.Address;
import capitec.branch.appointment.branch.domain.appointmentinfo.BranchAppointmentInfo;
import capitec.branch.appointment.day.domain.DayType;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDate;
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
        Map<DayType, BranchAppointmentInfo> branchAppointmentInfo,
        Map<LocalDate, Set<Staff>> weeklyStaff,
        Address address,
        @Column("created_at")
        LocalDateTime createdAt,
        @Column("last_modified_date")
        LocalDateTime updatedAt
        ) {
}
