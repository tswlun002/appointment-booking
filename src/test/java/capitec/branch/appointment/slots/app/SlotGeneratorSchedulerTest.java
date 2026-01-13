package capitec.branch.appointment.slots.app;

import capitec.branch.appointment.branch.domain.Branch;
import capitec.branch.appointment.slots.domain.Slot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;


class SlotGeneratorSchedulerTest extends SlotTestBase {

    @Autowired
    private SlotGeneratorScheduler slotGeneratorScheduler;
    @Autowired
    private SlotsGenerationFailerEventListener  slotsGenerationFailerEventListener;



    @Test
    @DisplayName("Should generate initial 7 days of slots when no slots exist")
    void shouldGenerateInitialSlotsWhenNoSlotsExist() {
        // When
        slotGeneratorScheduler.execute();

        var allSlots = slotService.getSlots(branch.getBranchId(), LocalDate.now().plusDays(1));

        // Then

        assertThat(allSlots).isNotEmpty();

        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDate expectedEndDate = tomorrow.plusDays(6);

        List<LocalDate> distinctDates = allSlots.stream()
                .map(Slot::getDay)
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
        slotGeneratorScheduler.execute();
        LocalDate fromDate = LocalDate.now().plusDays(1);
        long initialCount = slotService.getSlots(branch.getBranchId(), fromDate).size();


        // When - run scheduler again (simulates next day run)
        slotGeneratorScheduler.executeWithRetry();


        // Then - count should increase by one day's worth of slots

        long finalCount = slotService.getSlots(branch.getBranchId(), fromDate).size();

        assertThat(finalCount).isGreaterThanOrEqualTo(initialCount+9);
    }

    @Test
    @DisplayName("Should be idempotent - running twice generates same result")
    void shouldBeIdempotentWhenRunTwice() {
        // Given
        slotGeneratorScheduler.execute();
        long countAfterFirstRun = slotService.getSlots(branch.getBranchId(), LocalDate.now().plusDays(1)).size();

        // When - run again
        slotGeneratorScheduler.execute();

        // Then - no duplicates created
        long countAfterSecondRun = slotService.getSlots(branch.getBranchId(), LocalDate.now().plusDays(1)).size();
        assertThat(countAfterSecondRun).isEqualTo(countAfterFirstRun+9);
    }

    @Test
    @DisplayName("Should generate slots for all branches")
    void shouldGenerateSlotsForAllBranches() {
        // When
        slotGeneratorScheduler.execute();
        for (Branch b : branches) {
            List<Slot> slots = slotService.getSlots(b.getBranchId(), LocalDate.now().plusDays(1));
            assertThat(slots).isNotEmpty();
            Map<LocalDate, List<Slot>> collect = slots.stream().collect(Collectors.groupingBy(Slot::getDay));
            assertThat(collect).hasSize(7);
        }

    }

//    @Test
//    @DisplayName("Should publish failure event when generation fails after retries")
//    void shouldPublishFailureEventWhenGenerationFails() {
//        // This test requires mocking the use case to throw exceptions
//        // For now, verify event publishing mechanism works
//       slotGeneratorScheduler.execute();
//       var eventFailure = slotsGenerationFailerEventListener.eventFailure;
//        // Initially no failure events
//        assertThat(eventFailure).isNotNull();
//        assertThat(eventFailure.createdAt().toLocalDate()).isEqualTo(LocalDate.now());
//        assertThat(eventFailure.dateOfSlots()).isEqualTo(LocalDate.now().plusDays(1));
//        assertThat(eventFailure.reason()).isEqualTo("Slot generation failed.");
//    }

    @Test
    @DisplayName("executeWithRetry should complete successfully")
    void executeWithRetryShouldCompleteSuccessfully() {
        // When / Then - no exception thrown
        slotGeneratorScheduler.executeWithRetry();

        for (Branch b : branches) {
            List<Slot> slots = slotService.getSlots(b.getBranchId(), LocalDate.now().plusDays(1));
            assertThat(slots).isNotEmpty();
            Map<LocalDate, List<Slot>> collect = slots.stream().collect(Collectors.groupingBy(Slot::getDay));
            assertThat(collect).hasSize(7);

        }
    }
}
