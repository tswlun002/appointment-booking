package capitec.branch.appointment.slots.infrastructure.dao;

import capitec.branch.appointment.utils.IdStore;
import capitec.branch.appointment.slots.domain.SlotService;
import capitec.branch.appointment.slots.domain.Slot;
import capitec.branch.appointment.slots.domain.SlotStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Validated
@Slf4j
public class SlotDaoImpl implements SlotService {

    private final SloRepository sloRepository;
    private final SlotMapper slotMapper;
    private final IdStore idStore;

    @Override
    @Transactional
    public void save( List<Slot> slots) {

        if(slots == null || slots.isEmpty()) {
            log.error("Slot storage trying to save is null or empty");
            throw new IllegalArgumentException("Slot cannot be null");
        }

        try{

            var slotsEntities = slots.stream().map(slotMapper::toEntity).toList();

            idStore.setIdList(slotsEntities.stream().map(SlotEntity::id).toList());
            sloRepository.saveAll(slotsEntities);

        } catch (Exception e) {

            log.error("Could not save slot into DB", e);
            throw e;
        }
    }

    @Override
    public List<Slot> getDailySlot(String branchId,LocalDate day) {
        return sloRepository.dailySlot(branchId,day)
                .stream().map(slotMapper::toDomain)
                .toList();
    }

    @Override
    public List<Slot> getNext7DaySlots(String branchId,LocalDate date) {
         return getNext7DaySlots(branchId,date,null);
    }

    @Override
    public List<Slot> getNext7DaySlots(String branchId,LocalDate date, SlotStatus status) {
        String status1 = status == null ? null : status.name();
        return  sloRepository.next7DaySlots(branchId,date, status1)
                .stream()
                .map(slotMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public boolean cleanUpSlot(UUID id) {

        try {

           return sloRepository.deleteSlotEntitiesBySlotId(id.toString())==1;

        }
        catch (Exception e) {

            log.error("Could not delete slot into DB", e);
            throw e;
        }
    }

}
