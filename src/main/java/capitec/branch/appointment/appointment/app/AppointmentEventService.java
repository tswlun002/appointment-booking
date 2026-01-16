package capitec.branch.appointment.appointment.app;

import capitec.branch.appointment.appointment.app.dto.AppointmentBookedEvent;
import capitec.branch.appointment.appointment.app.dto.AppointmentStateChangedEvent;
import capitec.branch.appointment.appointment.app.dto.CustomerCanceledAppointmentEvent;
import capitec.branch.appointment.appointment.app.dto.CustomerRescheduledAppointmentEvent;

public interface AppointmentEventService {


    void publishEvent(AppointmentBookedEvent event);
    void publishEvent(AppointmentStateChangedEvent event);
    void publishEvent(CustomerCanceledAppointmentEvent event);
    void publishEvent(CustomerRescheduledAppointmentEvent event);
}
