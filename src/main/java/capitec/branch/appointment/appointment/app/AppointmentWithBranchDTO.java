package capitec.branch.appointment.appointment.app;

import capitec.branch.appointment.appointment.domain.Appointment;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO that contains appointment data enriched with branch information.
 */
public record AppointmentWithBranchDTO(
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
) {
    public static AppointmentWithBranchDTO from(Appointment appointment, String branchName, String branchAddress) {
        return new AppointmentWithBranchDTO(
                appointment.getId(),
                appointment.getSlotId(),
                appointment.getBranchId(),
                branchName,
                branchAddress,
                appointment.getCustomerUsername(),
                appointment.getServiceType(),
                appointment.getStatus().name(),
                appointment.getReference(),
                appointment.getDateTime(),
                appointment.getCreatedAt(),
                appointment.getUpdatedAt(),
                appointment.getCheckedInAt(),
                appointment.getInProgressAt(),
                appointment.getCompletedAt(),
                appointment.getTerminatedAt(),
                appointment.getTerminatedBy(),
                appointment.getTerminationReason() != null ? appointment.getTerminationReason().name() : null,
                appointment.getTerminationNotes(),
                appointment.getAssignedConsultantId(),
                appointment.getServiceNotes(),
                appointment.getPreviousSlotId(),
                appointment.getRescheduleCount()
        );
    }
}
