package capitec.branch.appointment.appointment.infrastructure.adapter;

import capitec.branch.appointment.appointment.app.AppointmentEventService;
import capitec.branch.appointment.appointment.domain.AppointmentStatus;
import capitec.branch.appointment.event.app.port.appointment.*;
import capitec.branch.appointment.sharekernel.EventTrigger;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AppointmentNotificationAdapter implements AppointmentEventService {

    private final ApplicationEventPublisher publisher;


    public void publishEventBookAppointment( UUID id, String reference, String branchId,
                              String customerUsername, LocalDate day, LocalTime startTime, LocalTime endTime,
                              LocalDateTime occurredAt) {
        publisher.publishEvent(new AppointmentBookedEvent(
                id, reference, branchId, customerUsername,
                day, startTime, endTime, occurredAt));
    }

    @Override
    public void publishEventChangeStatus( UUID appointmentId, String reference, String customerUsername, String branchId,
                              AppointmentStatus previousState, AppointmentStatus appointmentStatus, EventTrigger triggeredBy,
                               LocalDateTime occurredAt) {
        this.publishEventChangeStatus(appointmentId,reference,customerUsername,
                branchId,previousState,appointmentStatus,triggeredBy,occurredAt, Collections.emptyMap());

    }

    @Override
    public void publishEventChangeStatus(UUID appointmentId, String reference, String customerUsername,
                                         String branchId, AppointmentStatus previousState, AppointmentStatus
                                                     appointmentStatus, EventTrigger triggeredBy, LocalDateTime occurredAt
            , Map<String, Object> otherData) {
        if (appointmentStatus == AppointmentStatus.CANCELLED) {
            publisher.publishEvent(new StaffCanceledAppointmentEvent(
                    appointmentId,
                    reference,
                    customerUsername,
                    branchId,
                    previousState.name(),
                    appointmentStatus.name(),
                    triggeredBy,
                    occurredAt
            ));
        }
        else {
            publisher.publishEvent(AppointmentStateChangedEvent.transition(
                    appointmentId,
                    reference,
                    customerUsername,
                    branchId,
                    previousState.name(),
                    appointmentStatus.name(),
                    triggeredBy,
                    Collections.emptyMap()
            ));
        }

    }

    @Override
    public void publishCustomerCanceledAppointment(UUID appointmentId,
                                                String reference,
                                                String customerUsername,
                                                String branchId,
                                                AppointmentStatus previousState,
                                                AppointmentStatus appointmentStatus,
                                                EventTrigger triggeredBy,
                                                LocalDateTime occurredAt) {
        publisher.publishEvent(new CustomerCanceledAppointmentEvent(
                appointmentId,
                reference,
                customerUsername,
                branchId,
                previousState.name(),
                appointmentStatus.name(),
                triggeredBy,
                occurredAt
        ));
    }

    @Override
    public void publishStaffCanceledAppointment(UUID appointmentId, String reference, String customerUsername, String branchId,
                                                AppointmentStatus previousState, AppointmentStatus appointmentStatus,
                                                EventTrigger triggeredBy, LocalDateTime occurredAt, Map<String, Object> otherData) {
        publishEventChangeStatus(appointmentId,reference,customerUsername,branchId,previousState,
                appointmentStatus,triggeredBy,occurredAt,otherData);
    }

    @Override
    public void publishEventReschedule( UUID appointmentId,
                              String reference,
                              String customerUsername,
                              AppointmentStatus previousState,
                              AppointmentStatus appointmentStatus,
                              String branchId,
                                        EventTrigger triggeredBy,
                               LocalDateTime occurredAt) {
        publisher.publishEvent(new CustomerRescheduledAppointmentEvent(
                appointmentId,
                reference,
                customerUsername,
                previousState.name(),
                appointmentStatus.name(),
                branchId,
                triggeredBy,
                occurredAt
        ));
    }
}
