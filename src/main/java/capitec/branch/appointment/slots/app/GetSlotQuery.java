package capitec.branch.appointment.slots.app;

import capitec.branch.appointment.slots.domain.Slot;
import capitec.branch.appointment.slots.domain.SlotService;
import capitec.branch.appointment.utils.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Slf4j
@UseCase
@Validated
@RequiredArgsConstructor
public class GetSlotQuery {
    private final SlotService slotService;


    public Slot execute(UUID slotId) {

        return slotService.getSlot(slotId)
                .orElseThrow(()->{
                    log.error("Slot not found: {}", slotId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Slot not found");
                });
    }
}
