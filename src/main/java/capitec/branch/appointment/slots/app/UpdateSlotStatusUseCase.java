package capitec.branch.appointment.slots.app;

import capitec.branch.appointment.exeption.EntityAlreadyExistException;
import capitec.branch.appointment.slots.domain.SlotService;
import capitec.branch.appointment.utils.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.UUID;

@Slf4j
@UseCase
@Validated
@RequiredArgsConstructor
public class UpdateSlotStatusUseCase {

    private final SlotService slotService;

    public  void  execute(SlotStatusTransitionAction transitionAction) {

        UUID slotId = transitionAction.getId();
        var slot = slotService.getSlot(slotId)
                .orElseThrow(()->{
                    log.error("Slot not found for id: {}", slotId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND,"Slot not found.");
                });

      slot = transitionAction.execute(slot);

      try{

          slotService.save(List.of(slot));
      }
      catch (EntityAlreadyExistException ex){
          log.error("Failure to book slot with id: {}", slotId,ex);
          throw  new ResponseStatusException(HttpStatus.CONFLICT, "Slot is already booked.", ex);
      }
      catch (Exception e) {

          log.error("Could not update slot status for id: {}", slotId, e);
          throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error.");
      }

    }
}
