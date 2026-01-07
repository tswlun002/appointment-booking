package capitec.branch.appointment.slots.app;

import capitec.branch.appointment.slots.domain.Slot;
import capitec.branch.appointment.slots.domain.SlotService;
import capitec.branch.appointment.slots.domain.SlotStatus;
import capitec.branch.appointment.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@UseCase
@Validated
@RequiredArgsConstructor
public class GetNext7DaySlotsQuery {

    private final SlotService slotStorage;

    /**
     * Retrieves all scheduled slots for the next 7 days starting from a given date.
     * The results are grouped by date.
     * @param date The start date.
     * @return A map of dates to a list of slots.
     */
    public Map<LocalDate, List<Slot>> execute(String branchId,LocalDate date) {
        List<Slot> slots = slotStorage.getNext7DaySlots(branchId,date);
        return slots.stream().collect(Collectors.groupingBy(Slot::getDay));
    }

    /**
     * Retrieves scheduled slots for the next 7 days, optionally filtering by status.
     * @param date The start date.
     * @param status The status to filter. True means the slot is booked. False means the slot is not booked(AVAILABLE)
     * @return A map of dates to a list of filtered slots.
     */
    public Map<LocalDate, List<Slot>> execute(String branchId,LocalDate date, SlotStatus status) {
        List<Slot> slots = slotStorage.getNext7DaySlots(branchId,date, status);
        return slots.stream().collect(Collectors.groupingBy(Slot::getDay));
    }
}