package capitec.branch.appointment.appointment.app;

import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class AppointEventListenerTest {
    AppointmentStateChangedEvent bookedEvent;

    @EventListener(AppointmentStateChangedEvent.class)
    public void handle(AppointmentStateChangedEvent event){
        this.bookedEvent = event;
    }
}
