package capitec.branch.appointment.appointment.infrastructure.dao;

import org.springframework.data.annotation.*;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;
import java.time.LocalDateTime;
import java.util.UUID;

@Table("appointment")
record AppointmentEntity(
        @Id
        UUID id,
        @Column("slot_id")
        UUID slotId,
        @Column("branch_id")
        String branchId,
        @Column("customer_username")
        String customerUsername,
        @Column("service_type")
        String serviceType,
        String status,
        String reference,
        @Column("date_time")
        LocalDateTime dateTime,
        @Version
        int version,

        @Column("created_at")
        LocalDateTime createdAt,
        @Column("updated_at")
        LocalDateTime updatedAt,
        @Column("checked_in_at")
        LocalDateTime checkedInAt,
        @Column("in_progress_at")
        LocalDateTime inProgressAt,
        @Column("completed_at")
        LocalDateTime completedAt,
        @Column("terminated_at")
        LocalDateTime terminatedAt,

        // Audit Trail
        @Column("terminated_by")
        String terminatedBy,
        @Column("termination_reason")
        String terminationReason,
        @Column("termination_notes")
        String terminationNotes,

        // Service Execution
        @Column("assigned_consultant_id")
        String assignedConsultantId,
        @Column("service_notes")
        String serviceNotes,

        // Rescheduling Support
        @Column("previous_slot_id")
        UUID previousSlotId,
        @Column("reschedule_count")
        int rescheduleCount,


        @ReadOnlyProperty
        Long totalAppointmentsCount
) {
    public AppointmentEntity withVersion(int version) {
        return  new AppointmentEntity(this.id,this.slotId,this.branchId,this.customerUsername,this.serviceType,this.status,reference,
                this.dateTime,version, this.createdAt,this.updatedAt,this.checkedInAt,this.inProgressAt,this.completedAt,
                this.terminatedAt, this.terminatedBy,this.terminationReason,this.terminationNotes,this.assignedConsultantId,
                this.serviceNotes,this.previousSlotId,this.rescheduleCount, this.totalAppointmentsCount);
    }
}