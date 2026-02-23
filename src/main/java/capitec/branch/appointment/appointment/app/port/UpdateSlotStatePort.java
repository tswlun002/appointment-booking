package capitec.branch.appointment.appointment.app.port;

import java.time.LocalDateTime;
import java.util.UUID;

public interface UpdateSlotStatePort {

    void reserve(UUID slotId, LocalDateTime timestamp);
    void release(UUID slotId,LocalDateTime timestamp);
    void reschedule(UUID OldSlotId,UUID newSlotId,LocalDateTime timestamp);

}
