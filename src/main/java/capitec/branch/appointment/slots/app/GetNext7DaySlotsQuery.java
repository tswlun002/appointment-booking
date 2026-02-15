package capitec.branch.appointment.slots.app;

import capitec.branch.appointment.slots.app.port.SlotQueryPort;
import capitec.branch.appointment.slots.domain.Slot;
import capitec.branch.appointment.slots.domain.SlotStatus;
import capitec.branch.appointment.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Query use case for retrieving available appointment slots for the next 7 days.
 *
 * <p>This query is used by customers when browsing available appointment times
 * at a specific branch. It returns slots grouped by date for easy display in
 * calendar views or date pickers.</p>
 *
 * <h2>Query Methods:</h2>
 * <ul>
 *   <li><b>execute(branchId, fromDay)</b> - Retrieves all slots regardless of status</li>
 *   <li><b>execute(branchId, fromDay, status)</b> - Retrieves slots filtered by status (e.g., AVAILABLE only)</li>
 * </ul>
 *
 * <h2>Response Format:</h2>
 * <p>Returns a {@code Map<LocalDate, List<Slot>>} where:</p>
 * <ul>
 *   <li><b>Key:</b> The date of the slots</li>
 *   <li><b>Value:</b> List of slots for that date, ordered by start time</li>
 * </ul>
 *
 * <h2>Example Usage:</h2>
 * <pre>
 * // Get all available slots for branch 470010 starting from today
 * Map&lt;LocalDate, List&lt;Slot&gt;&gt; slots = query.execute("470010", LocalDate.now(), SlotStatus.AVAILABLE);
 *
 * // Response structure:
 * // {
 * //   2026-02-15: [Slot(08:00-08:30), Slot(08:30-09:00), ...],
 * //   2026-02-16: [Slot(08:00-08:30), Slot(09:00-09:30), ...],
 * //   ...
 * // }
 * </pre>
 *
 * <h2>Slot Status Filter:</h2>
 * <ul>
 *   <li><b>AVAILABLE:</b> Slot has capacity for more bookings</li>
 *   <li><b>FULL:</b> Slot has reached maximum booking capacity</li>
 *   <li><b>EXPIRED:</b> Slot date/time has passed</li>
 * </ul>
 *
 * @see Slot
 * @see SlotStatus
 * @see SlotQueryPort
 */
@UseCase
@Validated
@RequiredArgsConstructor
public class GetNext7DaySlotsQuery {

    private final SlotQueryPort slotQueryPort;

    /**
     * Retrieves all scheduled slots for the next 7 days starting from a given dateOfSlots.
     * The results are grouped by dateOfSlots.
     * @param fromDay The start dateOfSlots.
     * @return A map of dates to a list of slots.
     */
    public Map<LocalDate, List<Slot>> execute(String branchId, LocalDate fromDay) {
        List<Slot> slots = slotQueryPort.findByBranchFromDate(branchId, fromDay);
        return slots.stream().collect(Collectors.groupingBy(Slot::getDay));
    }

    /**
     * Retrieves scheduled slots for the next 7 days, optionally filtering by status.
     * @param fromDay The start dateOfSlots.
     * @param status The status to filter. True means the slot is booked. False means the slot is not booked(AVAILABLE)
     * @return A map of dates to a list of filtered slots.
     */
    public Map<LocalDate, List<Slot>> execute(String branchId, LocalDate fromDay, SlotStatus status) {
        List<Slot> slots = slotQueryPort.findByBranchFromDateAndStatus(branchId, fromDay, status);
        return slots.stream().collect(Collectors.groupingBy(Slot::getDay));
    }
}