package capitec.branch.appointment.slots.app;

import capitec.branch.appointment.slots.domain.Slot;

import java.time.LocalDate;
import java.util.List;

public interface SlotStorage {
    void save(List<Slot> slot);

    List<Slot> dailySlot(LocalDate day);

    List<Slot>next7DaySlots(LocalDate date);

     List<Slot> next7DaySlots(LocalDate date, Boolean status);
}
