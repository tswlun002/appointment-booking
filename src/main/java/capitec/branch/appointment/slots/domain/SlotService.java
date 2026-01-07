package capitec.branch.appointment.slots.domain;

import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SlotService {
    @Transactional
    void save(List<Slot> slot);

    List<Slot> getDailySlot(String branchId,LocalDate day);

    List<Slot> getNext7DaySlots(String branchId,LocalDate fromDate);

     List<Slot> getNext7DaySlots(String branchId,LocalDate fromDate, SlotStatus status);
     @Transactional
    boolean cleanUpSlot(UUID id);

    Optional<Slot> getSlot(UUID slotId);

}
