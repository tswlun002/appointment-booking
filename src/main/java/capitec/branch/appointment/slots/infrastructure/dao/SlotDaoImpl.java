package capitec.branch.appointment.slots.infrastructure.dao;

import capitec.branch.appointment.exeption.OptimisticLockConflictException;
import capitec.branch.appointment.sharekernel.id.IdStore;
import capitec.branch.appointment.slots.app.port.SlotCleanupPort;
import capitec.branch.appointment.slots.app.port.SlotQueryPort;
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
public class SlotDaoImpl implements SlotService, SlotQueryPort, SlotCleanupPort {

    private final SloRepository sloRepository;
    private final SlotMapper slotMapper;
    private final IdStore idStore;

    @Override
    @Transactional
    public void save( List<Slot> slots) {

        if(slots == null || slots.isEmpty()) {
            log.error("Slots trying to save is null or empty");
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

    // ==================== SlotCleanupPort Implementation ====================

    @Override
    public boolean deleteSlot(UUID id) {
        try {
            return sloRepository.deleteSlotEntitiesBySlotId(id) == 1;
        } catch (Exception e) {
            log.error("Could not delete slot from DB", e);
            throw e;
        }
    }

    // ==================== SlotQueryPort Implementation ====================

    @Override
    public Optional<Slot> findById(UUID slotId) {
        return sloRepository.findById(slotId)
                .map(slotMapper::toDomain);
    }

    @Override
    public List<Slot> findByBranchAndDay(String branchId, LocalDate day) {
        return sloRepository.dailySlot(branchId, day)
                .stream().map(slotMapper::toDomain)
                .toList();
    }

    @Override
    public List<Slot> findByBranchFromDate(String branchId, LocalDate fromDate) {
        return findByBranchFromDateAndStatus(branchId, fromDate, null);
    }

    @Override
    public List<Slot> findByBranchFromDateAndStatus(String branchId, LocalDate fromDate, SlotStatus status) {
        String statusValue = status == null ? null : status.name();
        return sloRepository.nextDaySlots(branchId, fromDate, statusValue)
                .stream()
                .map(slotMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<LocalDate> findLatestGeneratedSlotDate(LocalDate fromDate) {
        try {
            return sloRepository.getLastestGeneratedSlotDate(fromDate);
        } catch (Exception e) {
            log.error("Could not get last generated slot from DB", e);
            throw e;
        }
    }

}
