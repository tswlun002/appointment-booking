package capitec.branch.appointment.appointment.app;

import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class AppointEventListenerTest {
    AppointmentStateChangedEvent bookedEvent;
    AppointmentBookedEvent bookedEvent2;

    @EventListener(AppointmentStateChangedEvent.class)
    public void handle(AppointmentStateChangedEvent event){
        this.bookedEvent = event;
    }
    @EventListener(AppointmentBookedEvent.class)
    public void handle(AppointmentBookedEvent event){
        this.bookedEvent2 = event;
    }
}
