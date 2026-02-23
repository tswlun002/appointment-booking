package capitec.branch.appointment.notification.domain;

import java.time.LocalDateTime;

public record AppointmentStatusUpdatesEmail(
        String fullname,
        String email,
        String customerUsername,
        String branchId,
        String reference,
        String fromState,
        String toState,
        Notification.AppointmentEventType eventType,
        String traceId,
        LocalDateTime createdAt,
        String triggeredBy

) implements Email<Notification.AppointmentEventType>{
}
