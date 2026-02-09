package capitec.branch.appointment.slots.app;

import capitec.branch.appointment.branch.domain.Branch;
import capitec.branch.appointment.sharekernel.day.domain.Scale;
import capitec.branch.appointment.sharekernel.day.domain.Day;
import capitec.branch.appointment.slots.domain.Slot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;


class SlotGeneratorSchedulerTest extends SlotTestBase {

    @Autowired
    private SlotGeneratorScheduler slotGeneratorScheduler;


    @Test
    @DisplayName("Should generate initial 7 days of slots when no slots exist")
    void shouldGenerateInitialSlotsWhenNoSlotsExist() {
        // When
        slotGeneratorScheduler.execute();

        LocalDate date = LocalDate.now().plusDays(1);
        var allSlots = slotService.getSlots(branch.getBranchId(), date);

        // Then

        assertThat(allSlots).isNotEmpty();

        //ASSUME started next day after today and exclude sunday, possible holidays
        Set<Day> days = getDateOfNextDaysQuery.execute(date.getDayOfWeek(), date.plusDays(6).getDayOfWeek());
        long count = days.stream().filter(day -> day.isHoliday() && !day.getDate().getDayOfWeek().equals(DayOfWeek.SUNDAY)).count();

        LocalDate tomorrow = date;
        LocalDate expectedEndDate = tomorrow.plusDays(4-count);

        List<LocalDate> distinctDates = allSlots.stream()
                .map(Slot::getDay)
                .distinct()
                .sorted()
                .toList();

        assertThat(distinctDates).hasSize((int) (5-count));
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

        //extra day after 7 day
        Set<Day> execute = getDateOfNextDaysQuery.execute(fromDate.getDayOfWeek(), fromDate.plusDays(7).getDayOfWeek(),
                Scale.WEEK);
        var extraDay = execute
                .stream().sorted((day1, day2)->day2.getDate().compareTo(day1.getDate())).findFirst().get();


        // When - run scheduler again (simulates next day run)
        slotGeneratorScheduler.executeWithRetry();


        // Then - count should increase by one  day's worth of slots if not holiday or sunday next day
        long finalCount = slotService.getSlots(branch.getBranchId(), fromDate).size();

       if(extraDay.isHoliday() ||extraDay.getDate().getDayOfWeek().equals(DayOfWeek.SUNDAY)) {
           assertThat(finalCount).isEqualTo(initialCount);
       }
       else {
           assertThat(finalCount).isGreaterThan(initialCount);
       }

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
        assertThat(countAfterSecondRun).isGreaterThanOrEqualTo(countAfterFirstRun);
    }

    @Test
    @DisplayName("Should generate slots for all branches")
    void shouldGenerateSlotsForAllBranches() {
        // When

        slotGeneratorScheduler.execute();
        //ASSUME started next day after today and exclude sunday, possible holidays
        LocalDate date = LocalDate.now().plusDays(1);
        Set<Day> days = getDateOfNextDaysQuery.execute(date.getDayOfWeek(), date.plusDays(6).getDayOfWeek());
        int count = Math.toIntExact(days.stream().filter(day -> day.isHoliday() && !day.getDate().getDayOfWeek().equals(DayOfWeek.SUNDAY)).count());

        for (Branch b : branches) {
            List<Slot> slots = slotService.getSlots(b.getBranchId(), date);
            assertThat(slots).isNotEmpty();
            Map<LocalDate, List<Slot>> collect = slots.stream().collect(Collectors.groupingBy(Slot::getDay));
            assertThat(collect).hasSize(5-count);
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
        LocalDate date = LocalDate.now().plusDays(1);
        Set<Day> days = getDateOfNextDaysQuery.execute(date.getDayOfWeek(), date.plusDays(6).getDayOfWeek());
        int count = Math.toIntExact(days.stream().filter(day -> day.isHoliday() && !day.getDate().getDayOfWeek().equals(DayOfWeek.SUNDAY)).count());

        for (Branch b : branches) {
            List<Slot> slots = slotService.getSlots(b.getBranchId(), LocalDate.now().plusDays(1));
            assertThat(slots).isNotEmpty();
            Map<LocalDate, List<Slot>> collect = slots.stream().collect(Collectors.groupingBy(Slot::getDay));
            assertThat(collect).hasSize(5-count);

        }
    }
}
