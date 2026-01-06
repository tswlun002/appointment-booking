package capitec.branch.appointment.slots.domain;

import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

public interface SlotService {
    @Transactional
    void save(List<Slot> slot);

    List<Slot> dailySlot(LocalDate day);

    List<Slot>next7DaySlots(LocalDate date);

     List<Slot> next7DaySlots(LocalDate date, Boolean status);
     @Transactional
    boolean cleanUpSlot(int number);
}
