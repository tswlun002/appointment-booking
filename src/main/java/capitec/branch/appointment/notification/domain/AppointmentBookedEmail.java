package capitec.branch.appointment.notification.domain;

import java.time.LocalDate;
import java.time.LocalTime;

public record AppointmentBookedEmail(
        String fullname,
        String email,
        String customerUsername,
        String branchId,
        String reference,
        LocalDate day,
        LocalTime startTime,
        LocalTime endTime,
        Notification.AppointmentEventType eventType,
        String traceId

)  implements Email<Notification.AppointmentEventType>{
}
