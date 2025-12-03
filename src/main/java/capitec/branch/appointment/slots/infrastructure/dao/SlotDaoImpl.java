package capitec.branch.appointment.slots.infrastructure.dao;

import capitec.branch.appointment.slots.app.SlotStorage;
import capitec.branch.appointment.slots.domain.Slot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Validated
@Slf4j
public class SlotDaoImpl implements SlotStorage {

    private final SloRepository sloRepository;
    private final SlotMapper slotMapper;

    @Override
    public void save( List<Slot> slots) {

        if(slots == null || slots.isEmpty()) {
            log.error("Slot storage trying to save is null or empty");
            throw new IllegalArgumentException("Slot cannot be null");
        }

        try{

            var slotsEntities = slots.stream().map(slotMapper::toEntity).toList();

            sloRepository.saveAll(slotsEntities);

        } catch (Exception e) {

            log.error("Could not save slot into DB", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Slot> dailySlot(LocalDate day) {
        return sloRepository.dailySlot(day);
    }

    @Override
    public List<Slot> next7DaySlots(LocalDate date) {
         return next7DaySlots(date,null);
    }

    @Override
    public List<Slot> next7DaySlots(LocalDate date, Boolean status) {
        return  sloRepository.next7DaySlots(date, status);
    }

}
