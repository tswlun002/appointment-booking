package capitec.branch.appointment.appointment.infrastructure.adapter;

import capitec.branch.appointment.appointment.app.AppointmentEventService;
import capitec.branch.appointment.appointment.domain.AppointmentStatus;
import capitec.branch.appointment.event.app.port.AppointmentEventPort;
import capitec.branch.appointment.appointment.app.dto.AppointmentBookedEvent;
import capitec.branch.appointment.appointment.app.dto.AppointmentStateChangedEvent;
import capitec.branch.appointment.appointment.app.dto.CustomerCanceledAppointmentEvent;
import capitec.branch.appointment.appointment.app.dto.CustomerRescheduledAppointmentEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class AppointmentNotificationAdapter implements AppointmentEventService {

    private final AppointmentEventPort appointmentEventPort;

    @Override
    public void publishEvent(AppointmentBookedEvent event) {
        appointmentEventPort.publishEventAppointmentBooked(
                event.id(),
                event.reference(),
                event.branchId(),
                event.customerUsername(),
                event.day(),
                event.startTime(),
                event.endTime(),
                event.occurredAt()
        );
    }

    @Override
    public void publishEvent(AppointmentStateChangedEvent event) {
        if (event.toState() == AppointmentStatus.CANCELLED) {
            appointmentEventPort.publishEventCustomerCancelAppointment(
                    event.appointmentId(),
                    event.appointmentReference(),
                    event.customerUsername(),
                    event.branchId(),
                    event.fromState(),
                    event.toState(),
                    event.triggeredBy().name(),
                    event.occurredAt()
            );
        }
        else {
            appointmentEventPort.publishEventAttendAppointment(
                    event.appointmentId(),
                    event.appointmentReference(),
                    event.customerUsername(),
                    event.branchId(),
                    event.fromState(),
                    event.toState(),
                    event.triggeredBy().name(),
                    event.occurredAt()
            );
        }
    }

    @Override
    public void publishEvent(CustomerCanceledAppointmentEvent event) {
        appointmentEventPort.publishEventCustomerCancelAppointment(
                event.appointmentId(),
                event.reference(),
                event.customerUsername(),
                event.branchId(),
                event.previousState(),
                event.appointmentStatus(),
                event.triggeredBy().name(),
                event.createdAt()
        );
    }

    @Override
    public void publishEvent(CustomerRescheduledAppointmentEvent event) {
        appointmentEventPort.publishEventCustomerRescheduleAppointment(
                event.appointmentId(),
                event.reference(),
                event.customerUsername(),
                event.previousState(),
                event.appointmentStatus(),
                event.branchId(),
                event.triggeredBy().name(),
                event.createdAt()
        );
    }
}
