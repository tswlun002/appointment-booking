package capitec.branch.appointment.appointment.infrastructure.adapter;

import capitec.branch.appointment.appointment.app.port.SlotReservationPort;
import capitec.branch.appointment.slots.app.SlotStatusTransitionAction;
import capitec.branch.appointment.slots.app.UpdateSlotStatusUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SlotReservationAdapter implements SlotReservationPort {

    private final UpdateSlotStatusUseCase updateSlotStatusUseCase;

    @Override
    public void reserve(UUID slotId, LocalDateTime timestamp) {
        updateSlotStatusUseCase.execute(new SlotStatusTransitionAction.Book(slotId, timestamp));

    }
}
