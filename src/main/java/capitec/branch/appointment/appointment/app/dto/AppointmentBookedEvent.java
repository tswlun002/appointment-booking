package capitec.branch.appointment.appointment.app.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public record AppointmentBookedEvent(UUID id, String reference, String branchId, String customerUsername, LocalDate day,
                                     LocalTime startTime, LocalTime endTime, LocalDateTime occurredAt)  {

}
