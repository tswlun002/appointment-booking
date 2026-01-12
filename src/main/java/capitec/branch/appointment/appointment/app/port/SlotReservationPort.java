package capitec.branch.appointment.appointment.app.port;

import java.time.LocalDateTime;
import java.util.UUID;

public interface SlotReservationPort {
    void reserve(UUID slotId, LocalDateTime timestamp);
}
