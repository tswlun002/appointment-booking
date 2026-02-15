package capitec.branch.appointment.slots.app;


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
    private int MAX_BOOKING_CAPACITY = 2;

    @BeforeEach
    public void setUp() {

        setUpBranch();

        // 1. Create a new Slot domain object (Starts as AVAILABLE)
        LocalDate today = LocalDate.now().plusDays(1);
        LocalTime now = LocalTime.now();
        LocalTime startTime = now.plusMinutes(30);
        LocalTime endTime = now.plusHours(1);

        // Time that has passed the slot's start time, required by domain logic
        pastTime = LocalDateTime.of(today, startTime).plusSeconds(1);

        Slot newSlot = new Slot(today, startTime, endTime, MAX_BOOKING_CAPACITY, branch.getBranchId());

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
        Slot beforeSlot = slotQueryPort.findById(existingSlotId).orElseThrow();
        useCase.execute(new SlotStatusTransitionAction.Book(existingSlotId, LocalDateTime.now()));

        // Assert: Read the updated state directly from the database
        Slot updatedSlot = slotQueryPort.findById(existingSlotId).orElseThrow();
        assertThat(updatedSlot.getStatus())
                .as("Slot status should be updated to AVAILABLE")
                .isEqualTo(SlotStatus.AVAILABLE);
        assertThat(updatedSlot.getBookingCount())
                .as("Slot booking count should be updated to 1")
                .isEqualTo(1);

        // Optional: Check if the version field was incremented (Optimistic Locking)
        // If your SlotEntity uses 'int version', this should pass if persistence works.
        assertThat(updatedSlot.getVersion())
                .as("Slot version should be updated")
                .isGreaterThan(beforeSlot.getVersion());
    }
    @Test
    void execute_WhenMultipleBookAction_UntilSlotIsBook_Successfully() {
        // Act
        // Use the actual existing Slot ID and the action
        Slot originalSlot = slotQueryPort.findById(existingSlotId).orElseThrow();
        useCase.execute(new SlotStatusTransitionAction.Book(existingSlotId, LocalDateTime.now()));

        // Assert: Read the updated state directly from the database
        Slot firstBooking = slotQueryPort.findById(existingSlotId).orElseThrow();
        assertThat(firstBooking.getStatus())
                .as("Slot status should be updated to AVALABLE")
                .isEqualTo(SlotStatus.AVAILABLE);
        assertThat(firstBooking.getBookingCount())
                .as("Slot booking count should be updated to 1")
                .isEqualTo(1);

        useCase.execute(new SlotStatusTransitionAction.Book(existingSlotId, LocalDateTime.now()));
        Slot secondBooking = slotQueryPort.findById(existingSlotId).orElseThrow();

        assertThat(secondBooking.getStatus())
                .as("Slot status should be updated to BOOKED")
                .isEqualTo(SlotStatus.FULLY_BOOKED);
        assertThat(secondBooking.getBookingCount())
                .as("Slot booking count should be updated to "+MAX_BOOKING_CAPACITY)
                .isEqualTo(MAX_BOOKING_CAPACITY);


        // Optional: Check if the version field was incremented (Optimistic Locking)
        // If your SlotEntity uses 'int version', this should pass if persistence works.
        assertThat(firstBooking.getVersion())
                .as("Slot version should be updated")
                .isGreaterThan(originalSlot.getVersion());
        assertThat(secondBooking.getVersion())
                .as("Slot version should be updated")
                .isGreaterThan(firstBooking.getVersion());
    }

    @Test
    void execute_WhenReleaseAction_WhenOneSlotIsBooked_UpdatesStatusSuccessfully() {
        // Arrange: Manually set the slot state to BOOKED first to allow a valid Release transition
        Slot slotToBook = slotQueryPort.findById(existingSlotId).orElseThrow();
        //Add one booking
        slotToBook.book(LocalDateTime.now());

        slotService.save(List.of(slotToBook)); // Persist the BOOKED state

        // Act
        useCase.execute(new SlotStatusTransitionAction.Release(existingSlotId, LocalDateTime.now()));

        // Assert: Read the updated state directly from the database
        Slot releasedSlot = slotQueryPort.findById(existingSlotId).orElseThrow();
        assertThat(releasedSlot.getStatus())
                .as("Slot status should be AVAILABLE after releasing")
                .isEqualTo(SlotStatus.AVAILABLE);
        assertThat(releasedSlot.getBookingCount())
                .as("Slot booking count should be updated to zero")
                .isEqualTo(0);

        assertThat(releasedSlot.getVersion())
                .as("Slot version should be updated")
                .isEqualTo(3);
    }
    @Test
    void execute_WhenReleaseAction_WhenSlotIsFullyBooked_UpdatesStatusSuccessfully() {

        for (int i = 0; i < MAX_BOOKING_CAPACITY; i++) {

            // Arrange: Manually set the slot state to BOOKED first to allow a valid Release transition
            Slot slotToBook = slotQueryPort.findById(existingSlotId).orElseThrow();
            //Add one booking
            slotToBook.book(LocalDateTime.now());
            slotService.save(List.of(slotToBook)); // Persist the BOOKED state

        }

        // VERIFICATION
        Slot fullyBookedSlot = slotQueryPort.findById(existingSlotId).orElseThrow();
        assertThat(fullyBookedSlot.getStatus())
                .as("Slot status should be FULLY_BOOKED after releasing")
                .isEqualTo(SlotStatus.FULLY_BOOKED);

        // Act
        useCase.execute(new SlotStatusTransitionAction.Release(existingSlotId, LocalDateTime.now()));

        // Assert: Read the updated state directly from the database
        Slot releasedSlot = slotQueryPort.findById(existingSlotId).orElseThrow();
        assertThat(releasedSlot.getStatus())
                .as("Slot status should be AVAILABLE after releasing")
                .isEqualTo(SlotStatus.AVAILABLE);

        assertThat(releasedSlot.getBookingCount())
                .as("Slot booking count should be updated to 1")
                .isEqualTo(MAX_BOOKING_CAPACITY-1);

        assertThat(releasedSlot.getVersion())
                .as("Slot version should be updated")
                .isEqualTo(4);
    }

    @Test
    void execute_WhenBlockAction_UpdatesStatusInDatabase() {
        // Act
        useCase.execute(new SlotStatusTransitionAction.Block(existingSlotId, LocalDateTime.now()));

        // Assert
        Slot blockedSlot = slotQueryPort.findById(existingSlotId).orElseThrow();
        assertThat(blockedSlot.getStatus())
                .as("Slot status should be BLOCKED after blocking")
                .isEqualTo(SlotStatus.BLOCKED);
        assertThat(blockedSlot.getVersion())
                .as("Slot version should be updated")
                .isEqualTo(2);
    }
    @Test
    void execute_WhenBlockAction_OnExistingBooking_UpdatesStatusButKeepExistingBooking() {

        Slot slotToBook = slotQueryPort.findById(existingSlotId).orElseThrow();
        //Add one booking
        slotToBook.book(LocalDateTime.now());
        slotService.save(List.of(slotToBook));
        slotToBook = slotQueryPort.findById(existingSlotId).orElseThrow();
        // Act
        useCase.execute(new SlotStatusTransitionAction.Block(existingSlotId, LocalDateTime.now()));

        // Assert
        Slot blockedSlot = slotQueryPort.findById(existingSlotId).orElseThrow();
        assertThat(slotToBook.getBookingCount())
                .as("Existing booked slot booking count should be equal to blocked slot booking count")
                .isEqualTo(blockedSlot.getBookingCount());

        assertThat(blockedSlot.getStatus())
                .as("Slot status should be BLOCKED after blocking")
                .isEqualTo(SlotStatus.BLOCKED);
        assertThat(blockedSlot.getVersion())
                .as("Blocked Slot version should be updated and greater than booked slot version")
                .isEqualTo(3)
                .isGreaterThan(slotToBook.getVersion());
    }
    @Test
    void execute_WhenExpireAction_UpdatesStatusInDatabase() {
        // Act
        Slot beforeSlot = slotQueryPort.findById(existingSlotId).orElseThrow();
        useCase.execute(new SlotStatusTransitionAction.Expire(existingSlotId));

        // Assert
        Slot blockedSlot = slotQueryPort.findById(existingSlotId).orElseThrow();
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
        Slot beforeSlot = slotQueryPort.findById(existingSlotId).orElseThrow();

        assertThrows(ResponseStatusException.class,
                () -> useCase.execute(new SlotStatusTransitionAction.Release(existingSlotId, LocalDateTime.now())));

        // Assert Persistence: Check the database state to ensure it was NOT saved
        Slot slotAfterAttempt = slotQueryPort.findById(existingSlotId).orElseThrow();

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
    void execute_ConcurrentlyBookingSlot_Succeeds() throws InterruptedException {
        // Arrange: Ensure slot is AVAILABLE and save it to establish version=0
        // (This is handled by your @BeforeEach, but we'll confirm the initial version)
        Slot initialSlot = slotQueryPort.findById(existingSlotId).orElseThrow();
        long initialVersion = initialSlot.getVersion();

        // The action both threads will attempt: BOOK
        SlotStatusTransitionAction bookAction = new SlotStatusTransitionAction.Book(existingSlotId, LocalDateTime.now());

        // Create a container to capture any exception thrown by the threads
        // We expect one thread to throw the 409 ResponseStatusException
        // The Callable allows the worker thread to throw an exception back to the main thread.
        Callable<Void> task = () -> {
            useCase.execute( bookAction);
            return null;
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
            assertThat(capturedExceptions)
                    .as("Capture exceptions should be empty")
                    .isEmpty();

            // Assert Concurrency Failure

            // We expect exactly one thread to have thrown a ResponseStatusException (409 CONFLICT)
            long successCount = futures.size();

            // The core assertion for optimistic locking: one success, one conflict

            assertThat(successCount).as("All thread should succeed.").isEqualTo(2);

            // Assert Database State
            Slot finalSlot = slotQueryPort.findById(existingSlotId).orElseThrow();

            assertThat(finalSlot.getStatus())
                    .as("Final status must be FULLY_BOOKED after one successful transaction.")
                    .isEqualTo(SlotStatus.FULLY_BOOKED);

            // VERIFY booking count
            assertThat(finalSlot.getBookingCount())
                    .as("Slot booking count should be updated to "+MAX_BOOKING_CAPACITY)
                    .isEqualTo(MAX_BOOKING_CAPACITY);

            // The version should have been incremented exactly once (e.g., from 0 to 1)
            assertThat(finalSlot.getVersion())
                    .as("Version must be incremented twice by two successful threads.")
                    .isEqualTo(initialVersion + 2);
        };
    }

    @Test
    void execute_WhenSameSlotIsPickedAtSameTime_OneSucceedsAndOneFails() throws InterruptedException {
        // Arrange: Ensure slot is AVAILABLE and save it to establish version=0
        // (This is handled by your @BeforeEach, but we'll confirm the initial version)
        Slot initialSlot = slotQueryPort.findById(existingSlotId).orElseThrow();
        long initialVersion = initialSlot.getVersion();

        // The action both threads will attempt: BOOK
        SlotStatusTransitionAction bookAction = new SlotStatusTransitionAction.Book(existingSlotId, LocalDateTime.now());

        // Create a container to capture any exception thrown by the threads
        // We expect one thread to throw the 409 ResponseStatusException
        // The Callable allows the worker thread to throw an exception back to the main thread.
        Callable<Void> task = () -> {
            useCase.execute( bookAction);
            return null;
        };

        try(ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();) {


            // 3. Submit the two concurrent tasks
            List<Future<Void>> futures = new ArrayList<>();
            futures.add(executor.submit(task));
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
            assertThat(successCount).as("Exactly one thread should succeed.").isEqualTo(2);
            assertThat(exception.getStatusCode().value()).isEqualTo(409);
            assertThat(exception.getMessage()).isEqualTo("409 CONFLICT \"Slot is fully booked.\"");

            // Assert Database State
            Slot finalSlot = slotQueryPort.findById(existingSlotId).orElseThrow();

            assertThat(finalSlot.getStatus())
                    .as("Final status must be FULLY_BOOKED after one successful transaction.")
                    .isEqualTo(SlotStatus.FULLY_BOOKED);

            // VERIFY booking count
            assertThat(finalSlot.getBookingCount())
                    .as("Slot booking count should be updated to "+MAX_BOOKING_CAPACITY)
                    .isEqualTo(MAX_BOOKING_CAPACITY);

            // The version should have been incremented exactly once (e.g., from 0 to 1)
            assertThat(finalSlot.getVersion())
                    .as("Version must be incremented twice by two successful threads.")
                    .isEqualTo(initialVersion + 2);
        };
    }
}
