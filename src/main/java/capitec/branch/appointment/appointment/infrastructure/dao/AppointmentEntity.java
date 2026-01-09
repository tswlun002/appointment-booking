package capitec.branch.appointment.appointment.infrastructure.dao;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Table("appointment")
record AppointmentEntity(
        // Identity and Foreign Keys
        @Id
        UUID id,
        UUID slotId,
        String branchId,
        String customerUsername,
        String serviceType,

        // State Management
        // 🛑 CHANGED: Store enum as String
        String status,
        String bookingReference,

        // Concurrency Control
        @Version
        int version,

        // Timestamps
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime checkedInAt,
        LocalDateTime inProgressAt,
        LocalDateTime completedAt,
        LocalDateTime terminatedAt,

        // Audit Trail
        String terminatedBy,
        // 🛑 CHANGED: Store enum as String
        String terminationReason,
        String terminationNotes,

        // Service Execution
        String assignedConsultantId,
        String serviceNotes,

        // Rescheduling Support
        UUID previousSlotId,
        int rescheduleCount
) {

}