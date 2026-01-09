package capitec.branch.appointment.appointment.app;

import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class AppointEventListenerTest {
    AppointmentBookedEvent bookedEvent;

    @EventListener(AppointmentBookedEvent.class)
    public void handle(AppointmentBookedEvent event){
        this.bookedEvent = event;
    }
}
