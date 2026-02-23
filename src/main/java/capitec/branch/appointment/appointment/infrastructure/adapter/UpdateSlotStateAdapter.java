package capitec.branch.appointment.appointment.infrastructure.adapter;

import capitec.branch.appointment.appointment.app.port.UpdateSlotStatePort;
import capitec.branch.appointment.slots.app.SlotStatusTransitionAction;
import capitec.branch.appointment.slots.app.UpdateSlotStatusUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UpdateSlotStateAdapter implements UpdateSlotStatePort {

    private final UpdateSlotStatusUseCase updateSlotStatusUseCase;

    @Override
    public void reserve(UUID slotId, LocalDateTime timestamp) {
        updateSlotStatusUseCase.execute(new SlotStatusTransitionAction.Book(slotId, timestamp));
    }

    @Override
    public void release(UUID slotId,LocalDateTime timestamp) {

        updateSlotStatusUseCase.execute(new SlotStatusTransitionAction.Release(slotId,timestamp));
    }

    @Override
    public void reschedule(UUID OldSlotId, UUID newSlotId, LocalDateTime timestamp) {
        release(OldSlotId, timestamp);
        reserve(newSlotId, timestamp);
    }
}
