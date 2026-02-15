package capitec.branch.appointment.slots.app;

import capitec.branch.appointment.slots.app.port.SlotQueryPort;
import capitec.branch.appointment.utils.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.util.Optional;

@Slf4j
@UseCase
@Validated
@RequiredArgsConstructor
public class GetLastestGeneratedSlotDate {

    private final SlotQueryPort slotQueryPort;

    public Optional<LocalDate> execute(LocalDate fromDate) {

        try {
            return slotQueryPort.findLatestGeneratedSlotDate(fromDate);
        } catch (Exception e) {
            log.error("Failed to get lasted dateOfSlots of slots.", e);
            throw new RuntimeException("Failed to get lasted dateOfSlots of slots.",e);
        }
    }
}
