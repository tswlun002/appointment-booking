package capitec.branch.appointment.slots.app;

import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class SlotsGenerationFailerEventListener {

    protected  SlotGenerationSchedulerEventFailure eventFailure;
    @EventListener(SlotGenerationSchedulerEventFailure.class)
    public void onSlotsGenerationFailerEventListener(SlotGenerationSchedulerEventFailure eventListener) {
        this.eventFailure = eventListener;
    }
}
