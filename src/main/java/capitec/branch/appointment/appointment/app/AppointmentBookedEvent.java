package capitec.branch.appointment.appointment.app;

import org.springframework.context.ApplicationEvent;

import java.time.LocalTime;
import java.util.UUID;

public record AppointmentBookedEvent(UUID id, String reference, String branchId, String customerUsername, Object day,
                                     LocalTime startTime, LocalTime endTime)  {
}
