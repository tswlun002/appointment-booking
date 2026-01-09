package capitec.branch.appointment.slots.infrastructure.dao;

import capitec.branch.appointment.exeption.EntityAlreadyExistException;
import capitec.branch.appointment.exeption.OptimisticLockConflictException;
import capitec.branch.appointment.utils.IdStore;
import capitec.branch.appointment.slots.domain.SlotService;
import capitec.branch.appointment.slots.domain.Slot;
import capitec.branch.appointment.slots.domain.SlotStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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

            idStore.setIdList(slotsEntities.stream().map(s->s.id().toString()).toList());
            sloRepository.saveAll(slotsEntities);

        }
        catch (OptimisticLockingFailureException ex){
            log.error("Optimistic locking failure trying to save slots.\n",ex);
            throw new OptimisticLockConflictException(ex.getMessage(),ex);
        }
        catch (Exception e) {

            log.error("Could not save slot into DB", e);
            throw e;
        }
    }

    @Override
    public List<Slot> getDailySlot(String branchId,LocalDate fromDate) {
        return sloRepository.dailySlot(branchId,fromDate)
                .stream().map(slotMapper::toDomain)
                .toList();
    }

    @Override
    public List<Slot> getNext7DaySlots(String branchId,LocalDate date) {
         return getNext7DaySlots(branchId,date,null);
    }

    @Override
    public List<Slot> getNext7DaySlots(String branchId,LocalDate fromDate, SlotStatus status) {
        String status1 = status == null ? null : status.name();
        return  sloRepository.next7DaySlots(branchId,fromDate, status1)
                .stream()
                .map(slotMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public boolean cleanUpSlot(UUID id) {

        try {

           return sloRepository.deleteSlotEntitiesBySlotId(id)==1;

        }
        catch (Exception e) {

            log.error("Could not delete slot into DB", e);
            throw e;
        }
    }

    @Override
    public Optional<Slot> getSlot(UUID slotId) {

        return sloRepository.findById(slotId)
                .map(slotMapper::toDomain);
    }

}
