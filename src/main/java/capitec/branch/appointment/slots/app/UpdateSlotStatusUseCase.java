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

/**
 * Use case for updating slot status with optimistic locking and retry support.
 *
 * <p>This use case handles slot state transitions (reserve, release) with built-in
 * resilience for concurrent access scenarios. It implements optimistic locking
 * with automatic retry to handle high-traffic booking situations.</p>
 *
 * <h2>Supported Actions ({@link SlotStatusTransitionAction}):</h2>
 * <ul>
 *   <li><b>Reserve:</b> Increments booking count when customer books appointment</li>
 *   <li><b>Release:</b> Decrements booking count when appointment is cancelled/rescheduled</li>
 * </ul>
 *
 * <h2>Execution Flow:</h2>
 * <ol>
 *   <li>Fetches the slot by ID (throws 404 if not found)</li>
 *   <li>Executes the state transition on the slot domain object</li>
 *   <li>Persists the updated slot with optimistic locking</li>
 *   <li>On conflict, retries up to {@code MAX_RETRY_ATTEMPTS} times with exponential backoff</li>
 * </ol>
 *
 * <h2>Concurrency Handling:</h2>
 * <ul>
 *   <li><b>Optimistic Locking:</b> Uses version field to detect concurrent modifications</li>
 *   <li><b>Retry Strategy:</b> Up to 3 attempts with 50ms × retryCount backoff</li>
 *   <li><b>Conflict Resolution:</b> Re-fetches slot on each retry to get latest state</li>
 * </ul>
 *
 * <h2>Error Handling:</h2>
 * <ul>
 *   <li><b>NOT_FOUND (404)</b> - Slot doesn't exist</li>
 *   <li><b>CONFLICT (409)</b> - Max retries exceeded due to high traffic, or slot fully booked</li>
 *   <li><b>BAD_REQUEST (400)</b> - Invalid state transition (e.g., releasing unbooked slot)</li>
 *   <li><b>INTERNAL_SERVER_ERROR (500)</b> - Unexpected errors</li>
 * </ul>
 *
 * <h2>Example Use Cases:</h2>
 *
 * <p><b>1. Reserve Slot (Customer Books):</b></p>
 * <pre>
 * SlotStatusTransitionAction.Reserve action = new Reserve(slotId);
 * updateSlotStatusUseCase.execute(action);
 * // Slot booking count: 2 → 3
 * </pre>
 *
 * <p><b>2. Release Slot (Customer Cancels):</b></p>
 * <pre>
 * SlotStatusTransitionAction.Release action = new Release(slotId);
 * updateSlotStatusUseCase.execute(action);
 * // Slot booking count: 3 → 2
 * </pre>
 *
 * <h2>High Traffic Scenario:</h2>
 * <p>When multiple customers try to book the same slot simultaneously:</p>
 * <ol>
 *   <li>First request succeeds, increments booking count</li>
 *   <li>Second request detects version conflict</li>
 *   <li>Second request retries with fresh slot data</li>
 *   <li>If slot still has capacity, booking succeeds</li>
 *   <li>If slot is full, returns 409 Conflict with "Slot is fully booked"</li>
 * </ol>
 *
 * @see SlotStatusTransitionAction
 * @see SlotService
 * @see SlotQueryPort
 * @see OptimisticLockConflictException
 * @see SlotFullyBookedException
 */
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