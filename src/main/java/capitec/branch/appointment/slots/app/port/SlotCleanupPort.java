package capitec.branch.appointment.slots.app.port;

import java.util.UUID;

/**
 * Port for slot cleanup operations - used for test cleanup only.
 * Not a business operation.
 */
public interface SlotCleanupPort {

    boolean deleteSlot(UUID slotId);
}
