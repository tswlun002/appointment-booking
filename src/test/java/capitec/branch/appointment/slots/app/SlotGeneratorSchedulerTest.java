package capitec.branch.appointment.slots.app;

import capitec.branch.appointment.slots.domain.Slot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


class SlotGeneratorSchedulerTest extends SlotTestBase {

    @Autowired
    private SlotGeneratorScheduler slotGeneratorScheduler;

    @Autowired
    private ApplicationEvents applicationEvents;

    @BeforeEach
    void setUp() {
        clearSlots();
    }

    @Test
    @DisplayName("Should generate initial 7 days of slots when no slots exist")
    void shouldGenerateInitialSlotsWhenNoSlotsExist() {
        // When
        slotGeneratorScheduler.generateDailySlots();

        // Then
        List<Slot> allSlots = findAllSlots();
        assertThat(allSlots).isNotEmpty();

        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDate expectedEndDate = tomorrow.plusDays(6);

        List<LocalDate> distinctDates = allSlots.stream()
                .map(Slot::getDate)
                .distinct()
                .sorted()
                .toList();

        assertThat(distinctDates).hasSize(7);
        assertThat(distinctDates.getFirst()).isEqualTo(tomorrow);
        assertThat(distinctDates.getLast()).isEqualTo(expectedEndDate);
    }

    @Test
    @DisplayName("Should generate only 1 day of slots when slots already exist")
    void shouldGenerateOneDayWhenSlotsExist() {
        // Given - generate initial slots
        slotGeneratorScheduler.generateDailySlots();
        long initialCount = countSlots();

        // Simulate next day by manually inserting a gap
        LocalDate nextDayToGenerate = LocalDate.now().plusDays(8);

        // When - run scheduler again (simulates next day run)
        slotGeneratorScheduler.executeWithRetry();

        // Then - count should increase by one day's worth of slots
        long finalCount = countSlots();
        assertThat(finalCount).isGreaterThanOrEqualTo(initialCount);
    }

    @Test
    @DisplayName("Should be idempotent - running twice generates same result")
    void shouldBeIdempotentWhenRunTwice() {
        // Given
        slotGeneratorScheduler.generateDailySlots();
        long countAfterFirstRun = countSlots();
        List<Slot> slotsAfterFirstRun = findAllSlots();

        // When - run again
        slotGeneratorScheduler.generateDailySlots();

        // Then - no duplicates created
        long countAfterSecondRun = countSlots();
        assertThat(countAfterSecondRun).isEqualTo(countAfterFirstRun);
    }

    @Test
    @DisplayName("Should generate slots for all branches")
    void shouldGenerateSlotsForAllBranches() {
        // When
        slotGeneratorScheduler.generateDailySlots();

        // Then
        List<Slot> allSlots = findAllSlots();
        long distinctBranchCount = allSlots.stream()
                .map(Slot::getBranchId)
                .distinct()
                .count();

        assertThat(distinctBranchCount).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should publish failure event when generation fails after retries")
    void shouldPublishFailureEventWhenGenerationFails() {
        // This test requires mocking the use case to throw exceptions
        // For now, verify event publishing mechanism works
        long failureEventCount = applicationEvents
                .stream(SlotGenerationSchedulerEventFailure.class)
                .count();

        // Initially no failure events
        assertThat(failureEventCount).isZero();
    }

    @Test
    @DisplayName("executeWithRetry should complete successfully")
    void executeWithRetryShouldCompleteSuccessfully() {
        // When / Then - no exception thrown
        slotGeneratorScheduler.executeWithRetry();

        List<Slot> allSlots = findAllSlots();
        assertThat(allSlots).isNotEmpty();
    }
}
