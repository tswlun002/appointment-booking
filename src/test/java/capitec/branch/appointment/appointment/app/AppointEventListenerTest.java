package capitec.branch.appointment.appointment.app;

import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class AppointEventListenerTest {
    AppointmentStateChangedEvent bookedEvent;
    AppointmentBookedEvent bookedEvent2;
    CustomerCanceledAppointmentEvent canceledEvent;
    CustomerRescheduledAppointmentEvent rescheduledEvent;

    @EventListener(AppointmentStateChangedEvent.class)
    public void handle(AppointmentStateChangedEvent event){
        this.bookedEvent = event;
    }
    @EventListener(AppointmentBookedEvent.class)
    public void handle(AppointmentBookedEvent event){
        this.bookedEvent2 = event;
    }
    @EventListener(CustomerCanceledAppointmentEvent.class)
    public void handle(CustomerCanceledAppointmentEvent event){
        this.canceledEvent = event;
    }
    @EventListener(CustomerRescheduledAppointmentEvent.class)
    public void handle(CustomerRescheduledAppointmentEvent event){
        this.rescheduledEvent = event;
    }
}
