package capitec.branch.appointment.slots.app.port;

import capitec.branch.appointment.slots.domain.Slot;
import capitec.branch.appointment.slots.domain.SlotStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for read-only slot queries.
 * Queries are not business rules - they are data retrieval for presentation.
 */
public interface SlotQueryPort {

    Optional<Slot> findById(UUID slotId);

    List<Slot> findByBranchAndDay(String branchId, LocalDate day);

    List<Slot> findByBranchFromDate(String branchId, LocalDate fromDate);

    List<Slot> findByBranchFromDateAndStatus(String branchId, LocalDate fromDate, SlotStatus status);

    Optional<LocalDate> findLatestGeneratedSlotDate(LocalDate fromDate);
}
