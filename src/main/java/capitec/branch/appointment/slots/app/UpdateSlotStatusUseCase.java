package capitec.branch.appointment.slots.app;

import capitec.branch.appointment.exeption.OptimisticLockConflictException;
import capitec.branch.appointment.exeption.SlotFullyBookedException;
import capitec.branch.appointment.slots.app.port.SlotQueryPort;
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
    private final SlotQueryPort slotQueryPort;
    private static final int MAX_RETRY_ATTEMPTS = 3;

    public void execute(SlotStatusTransitionAction transitionAction) {

        UUID slotId = transitionAction.getId();
        int retryCount = 0;

        while (retryCount < MAX_RETRY_ATTEMPTS) {

            var slot = slotQueryPort.findById(slotId).orElseThrow(() -> {
                log.error("Slot not found for id: {}", slotId);
                return new ResponseStatusException(HttpStatus.NOT_FOUND, "Slot not found.");
            });
//9ba4a2b4-831a-459f-9900-f3b7a88e4537
            try {

                slot = transitionAction.execute(slot);
                slotService.save(List.of(slot));
                log.info("Successfully updated slot with id: {} after {} attempts", slotId, retryCount + 1);

                return;

            } catch (OptimisticLockConflictException ex) {

                retryCount++;
                log.warn("OptimisticLockConflictException on attempt {} for slot id: {}", retryCount, slotId);

                if (retryCount >= MAX_RETRY_ATTEMPTS) {
                    log.error("Max retry attempts reached for slot id: {}. Giving up.", slotId, ex);
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Unable to book slot due to high traffic. Please try again.", ex);
                }

                // Optional: Add exponential backoff
                retryBackOffThread(retryCount);
                log.info("Retrying slot booking for id: {} (attempt {})", slotId, retryCount + 1);

            }
            catch (SlotFullyBookedException ex) {

                log.warn("Slot with id: {} is fully booked. Current bookings: {}, Max capacity: {}", slotId, slot.getBookingCount(), slot.getMaxBookingCapacity());
                throw  new ResponseStatusException(HttpStatus.CONFLICT, "Slot is fully booked.",ex);
            }
            catch (IllegalStateException | IllegalArgumentException ex) {
                log.warn("Slot with id: {} is not booked", slotId, ex);
                throw  new  ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
            }
            catch (Exception e) {
                log.error("Unexpected error updating slot status for id: {}", slotId, e);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error.", e);
            }
        }
    }

    private void retryBackOffThread(int retryCount) {
        try {
            Thread.sleep(50L * retryCount);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Interrupted during retry.", ie);
        }

    }
}