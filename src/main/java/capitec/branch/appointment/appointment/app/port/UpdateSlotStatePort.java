package capitec.branch.appointment.appointment.app.port;

import java.time.LocalDateTime;
import java.util.UUID;

public interface UpdateSlotStatePort {
    void execute(UUID slotId, LocalDateTime timestamp);
}
