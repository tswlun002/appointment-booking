package capitec.branch.appointment.event.app.port;

import capitec.branch.appointment.appointment.domain.AppointmentStatus;
import capitec.branch.appointment.utils.Username;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public interface AppointmentEventPort {
    void publishEventAppointmentBooked(UUID id, String reference, String branchId, String customerUsername, LocalDate day,
                                       LocalTime startTime, LocalTime endTime,LocalDateTime occurredAt);

    void publishEventAttendAppointment(@NotNull UUID appointmentId,
                                       @NotBlank String appointmentReference,
                                       @Username String customerUsername,
                                       String branchId,
                                       AppointmentStatus fromState,
                                       @NotNull AppointmentStatus toState,
                                       String triggeredBy,
                                       @NotNull LocalDateTime occurredAt);


    void publishEventCustomerCancelAppointment(UUID appointmentId,
                                               String reference,
                                               String customerUsername,
                                               String branchId,
                                               AppointmentStatus previousState,
                                               AppointmentStatus appointmentStatus,
                                               String triggeredBy,
                                               @NotNull LocalDateTime occurredAt);


    void publishEventCustomerRescheduleAppointment(UUID appointmentId,
                                               String reference,
                                               String customerUsername,
                                               AppointmentStatus previousState,
                                               AppointmentStatus appointmentStatus,
                                               String branchId,
                                                   String triggeredBy,
                                                   @NotNull LocalDateTime occurredAt);
}
