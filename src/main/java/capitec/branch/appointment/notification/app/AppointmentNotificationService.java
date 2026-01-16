package capitec.branch.appointment.notification.app;

import capitec.branch.appointment.appointment.app.dto.AppointmentBookedEvent;
import capitec.branch.appointment.appointment.app.dto.AppointmentStateChangedEvent;
import capitec.branch.appointment.appointment.app.dto.CustomerCanceledAppointmentEvent;
import capitec.branch.appointment.appointment.app.dto.CustomerRescheduledAppointmentEvent;

public interface AppointmentNotificationService {

     void appointmentBooked(AppointmentBookedEvent event);
     void attendAppointment(AppointmentStateChangedEvent  event);
    void customerRescheduledAppointment(CustomerRescheduledAppointmentEvent event);

    void customerCanceledAppointment(CustomerCanceledAppointmentEvent event);
}
