package capitec.branch.appointment.appointment.app;

import capitec.branch.appointment.appointment.domain.AppointmentStatus;
import capitec.branch.appointment.utils.sharekernel.EventTrigger;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;

public interface AppointmentEventService {


     void publishEventBookAppointment(UUID id, String reference, String branchId, String customerUsername, LocalDate day,
                             LocalTime startTime, LocalTime endTime, LocalDateTime occurredAt);


    void publishEventChangeStatus(UUID appointmentId, String reference, String customerUsername, String branchId,
                      AppointmentStatus previousState, AppointmentStatus appointmentStatus, EventTrigger triggeredBy,
                      LocalDateTime occurredAt);
    void publishEventChangeStatus(UUID appointmentId, String reference, String customerUsername, String branchId,
                                  AppointmentStatus previousState, AppointmentStatus appointmentStatus, EventTrigger triggeredBy,
                                  LocalDateTime occurredAt, Map<String, Object> otherData);


    void publishCustomerCanceledAppointment(UUID appointmentId, String reference, String customerUsername, String branchId,
                                         AppointmentStatus previousState, AppointmentStatus appointmentStatus, EventTrigger triggeredBy,
                                         LocalDateTime occurredAt);

    void publishStaffCanceledAppointment(UUID appointmentId, String reference, String customerUsername, String branchId,
                                         AppointmentStatus previousState, AppointmentStatus appointmentStatus, EventTrigger triggeredBy,
                                         LocalDateTime occurredAt, Map<String, Object> otherData);

    void publishEventReschedule(UUID appointmentId, String reference, String customerUsername,
                      AppointmentStatus previousState, AppointmentStatus appointmentStatus,
                      String branchId, EventTrigger triggeredBy, LocalDateTime occurredAt);
}
