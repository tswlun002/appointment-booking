package capitec.branch.appointment.appointment.infrastructure.controller;

import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentResponse(
        UUID id,
        UUID slotId,
        String branchId,
        String branchName,
        String branchAddress,
        String customerUsername,
        String serviceType,
        String status,
        String reference,
        LocalDateTime dateTime,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime checkedInAt,
        LocalDateTime inProgressAt,
        LocalDateTime completedAt,
        LocalDateTime terminatedAt,
        String terminatedBy,
        String terminationReason,
        String terminationNotes,
        String assignedConsultantId,
        String serviceNotes,
        UUID previousSlotId,
        int rescheduleCount
) {}
