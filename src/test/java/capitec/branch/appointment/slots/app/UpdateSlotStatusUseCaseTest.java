package capitec.branch.appointment.slots.app;


import capitec.branch.appointment.exeption.EntityAlreadyExistException;
import capitec.branch.appointment.slots.domain.Slot;
import capitec.branch.appointment.slots.domain.SlotService;
import capitec.branch.appointment.slots.domain.SlotStatus;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UpdateSlotStatusUseCaseTest extends SlotTestBase {
    // Inject the component being tested
    @Autowired
    private UpdateSlotStatusUseCase useCase;

    // Inject the service for creating domain objects (if needed for complex setup)
    @Autowired
    private SlotService slotService;

    // --- Test Data ---
    private UUID existingSlotId;
    private LocalDateTime pastTime;

    @BeforeEach
    void setUp() {

        // 1. Create a new Slot domain object (Starts as AVAILABLE)
        LocalDate today = LocalDate.now();
        LocalTime startTime = LocalTime.now().minusMinutes(10);
        LocalTime endTime = startTime.plusHours(1);

        // Time that has passed the slot's start time, required by domain logic
        pastTime = LocalDateTime.of(today, startTime).plusSeconds(1);

        Slot newSlot = new Slot(today, startTime, endTime, 1, branchId);

        // 2. Save the Slot to the database via the service or repository
        //    (Service preferred to maintain transactional integrity if applicable)
        slotService.save(List.of(newSlot));

        existingSlotId = newSlot.getId();
    }

    // --- Test Cases ---

    @Test
    void execute_WhenSlotDoesNotExist_ThrowsNotFoundException() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> useCase.execute(new SlotStatusTransitionAction.Book(nonExistentId, pastTime)));

        assertThat(exception.getStatusCode().value())
                .as("Response status code should be 404")
                .isEqualTo(404);
        assertThat(exception.getReason())
                .as("Reason should be NOT_FOUND")
                .isEqualTo("Slot not found.");
    }

    @Test
    void execute_WhenBookAction_UpdatesStatusInDatabase() {
        // Act
        // Use the actual existing Slot ID and the action
        Slot beforeSlot = slotService.getSlot(existingSlotId).orElseThrow();
        useCase.execute(new SlotStatusTransitionAction.Book(existingSlotId, pastTime));

        // Assert: Read the updated state directly from the database
        Slot updatedSlot = slotService.getSlot(existingSlotId).orElseThrow();
        assertThat(updatedSlot.getStatus())
                .as("Slot status should be updated to BOOKED")
                .isEqualTo(SlotStatus.BOOKED);


        // Optional: Check if the version field was incremented (Optimistic Locking)
        // If your SlotEntity uses 'int version', this should pass if persistence works.
        assertThat(updatedSlot.getVersion())
                .as("Slot version should be updated")
                .isGreaterThan(beforeSlot.getVersion());
    }

    @Test
    void execute_WhenReleaseAction_UpdatesStatusInDatabase() {
        // Arrange: Manually set the slot state to BOOKED first to allow a valid Release transition
        Slot slotToBook = slotService.getSlot(existingSlotId).orElseThrow();
        slotToBook.book(pastTime);
        slotService.save(List.of(slotToBook)); // Persist the BOOKED state

        // Act
        useCase.execute(new SlotStatusTransitionAction.Release(existingSlotId, pastTime));

        // Assert: Read the updated state directly from the database
        Slot releasedSlot = slotService.getSlot(existingSlotId).orElseThrow();
        assertThat(releasedSlot.getStatus())
                .as("Slot status should be AVAILABLE after releasing")
                .isEqualTo(SlotStatus.AVAILABLE);

        assertThat(releasedSlot.getVersion())
                .as("Slot version should be updated")
                .isGreaterThan(slotToBook.getVersion());
    }

    @Test
    void execute_WhenBlockAction_UpdatesStatusInDatabase() {
        // Act
        Slot beforeSlot = slotService.getSlot(existingSlotId).orElseThrow();
        useCase.execute(new SlotStatusTransitionAction.Block(existingSlotId, pastTime));

        // Assert
        Slot blockedSlot = slotService.getSlot(existingSlotId).orElseThrow();
        assertThat(blockedSlot.getStatus())
                .as("Slot status should be BLOCKED after blocking")
                .isEqualTo(SlotStatus.BLOCKED);
        assertThat(blockedSlot.getVersion())
                .as("Slot version should be updated")
                .isGreaterThan(beforeSlot.getVersion());
    }
    @Test
    void execute_WhenExpireAction_UpdatesStatusInDatabase() {
        // Act
        Slot beforeSlot = slotService.getSlot(existingSlotId).orElseThrow();
        useCase.execute(new SlotStatusTransitionAction.Expire(existingSlotId));

        // Assert
        Slot blockedSlot = slotService.getSlot(existingSlotId).orElseThrow();
        assertThat(blockedSlot.getStatus())
                .as("Slot status should be EXPIRED after blocking")
                .isEqualTo(SlotStatus.EXPIRED);
        assertThat(blockedSlot.getVersion())
                .as("Slot version should be updated")
                .isGreaterThan(beforeSlot.getVersion());
    }

    @Test
    void execute_WhenInvalidDomainTransition_DoesNotCommitToDatabase() {
        // Arrange: Slot starts AVAILABLE. Try to Release it (invalid transition)

        // Act & Assert (Domain logic prevents save)
        Slot beforeSlot = slotService.getSlot(existingSlotId).orElseThrow();

        assertThrows(IllegalStateException.class,
                () -> useCase.execute(new SlotStatusTransitionAction.Release(existingSlotId, pastTime)));

        // Assert Persistence: Check the database state to ensure it was NOT saved
        Slot slotAfterAttempt = slotService.getSlot(existingSlotId).orElseThrow();

        // State should remain AVAILABLE, as the transaction should roll back
        // or fail before save.
        Assertions.assertThat(slotAfterAttempt.getStatus())
                .as("Slot status should be AVAILABLE after failed attempt")
                .isEqualTo(SlotStatus.AVAILABLE);
        assertThat(slotAfterAttempt.getVersion()).
                as("Slot version should not be updated")
                .isEqualTo(beforeSlot.getVersion());

    }

    @Test
    void execute_WhenSameSlotIsPickedAtSameTime_OneSucceedsAndOneFails() throws InterruptedException {
        // Arrange: Ensure slot is AVAILABLE and save it to establish version=0
        // (This is handled by your @BeforeEach, but we'll confirm the initial version)
        Slot initialSlot = slotService.getSlot(existingSlotId).orElseThrow();
        long initialVersion = initialSlot.getVersion();

        // The action both threads will attempt: BOOK
        SlotStatusTransitionAction bookAction = new SlotStatusTransitionAction.Book(existingSlotId, pastTime);

        // Create a container to capture any exception thrown by the threads
        // We expect one thread to throw the 409 ResponseStatusException
        // The Callable allows the worker thread to throw an exception back to the main thread.
        Callable<Void> task = () -> {
            useCase.execute( bookAction);
            return null; // Return Void on success
        };

        try(ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();) {


            // 3. Submit the two concurrent tasks
            List<Future<Void>> futures = new ArrayList<>();
            futures.add(executor.submit(task));
            futures.add(executor.submit(task));


            // Containers to capture results
            List<ResponseStatusException> capturedExceptions = new ArrayList<>();

            for (Future<Void> future : futures) {
                try {

                    future.get();
                } catch (ExecutionException e) {

                    capturedExceptions.add((ResponseStatusException) e.getCause());
                }
            }

            // Shut down the executor (essential to release resources)
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);


            // Assertion 1: Verify the failure count and type
            assertThat(capturedExceptions).hasSize(1);

            // Assert Concurrency Failure

            // We expect exactly one thread to have thrown a ResponseStatusException (409 CONFLICT)
            long successCount = futures.size()-capturedExceptions.size();

            // The core assertion for optimistic locking: one success, one conflict
            ResponseStatusException exception = capturedExceptions.getFirst();
            assertThat(successCount).as("Exactly one thread should succeed.").isEqualTo(1);
            assertThat(exception.getStatusCode().value()).isEqualTo(409);
            assertThat(exception.getMessage()).isEqualTo("409 CONFLICT \"Slot is already booked.\"");
            assertThat(exception.getCause()).isInstanceOf(EntityAlreadyExistException.class);

            // Assert Database State
            Slot finalSlot = slotService.getSlot(existingSlotId).orElseThrow();

            assertThat(finalSlot.getStatus())
                    .as("Final status must be BOOKED after one successful transaction.")
                    .isEqualTo(SlotStatus.BOOKED);

            // The version should have been incremented exactly once (e.g., from 0 to 1)
            assertThat(finalSlot.getVersion())
                    .as("Version must be incremented once by the successful thread.")
                    .isEqualTo(initialVersion + 1);
        };
    }
}