package capitec.branch.appointment.slots.app;

import capitec.branch.appointment.slots.domain.Slot;
import capitec.branch.appointment.slots.domain.SlotService;
import capitec.branch.appointment.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.util.List;

@UseCase
@Validated
@RequiredArgsConstructor
public class GetDailySlotsQuery {

    private final SlotService slotService;

    /**
     * Retrieves all scheduled slots for a specific day.
     * @param day The date to query.
     * @return A list of slots for the given day.
     */
    public List<Slot> execute(LocalDate day) {
        return slotService.dailySlot(day);
    }
}