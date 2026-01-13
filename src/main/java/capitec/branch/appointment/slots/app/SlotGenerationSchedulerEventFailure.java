package capitec.branch.appointment.slots.app;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SlotGenerationSchedulerEventFailure(
        String reason,
        LocalDateTime createdAt,
        LocalDate dateOfSlots
) {
}
