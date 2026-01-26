package capitec.branch.appointment.slots.app;

import capitec.branch.appointment.day.app.CheckHolidayQuery;
import capitec.branch.appointment.day.domain.Day;
import capitec.branch.appointment.slots.domain.Slot;
import capitec.branch.appointment.slots.domain.SlotStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static capitec.branch.appointment.slots.domain.Day.isWeekend;
import static org.assertj.core.api.Assertions.assertThat;


class GenerateSlotsUseCaseTest extends SlotTestBase {

    @Autowired
    private GenerateSlotsUseCase generateSlotsUseCase; // The Command under test
    
    // Need a Query to verify the results after the command runs
    @Autowired
    private GetNext7DaySlotsQuery getNext7DaySlotsQuery;
    
    // Need the CheckHolidayQuery to verify filtering logic
    @Autowired
    private CheckHolidayQuery checkHolidayQuery;




    /**
     * Integration test verifying the entire 7-day slot generation pipeline,
     * including holiday and weekend capacity logic.
     */
   @Test
    public void testCreateNext7DaySlots_GeneratesCorrectCountsPerDayType() {

        wireMockGetHolidayByYearAndCountryCode("2025", "ZA");

       LocalDate date = LocalDate.now().plusDays(1);

       generateSlotsUseCase.createNext7DaySlots(date,0);

       Set<Day> days = getDateOfNextDaysQuery.execute(date.getDayOfWeek(), date.plusDays(6).getDayOfWeek());
       long count = days.stream().filter(day -> day.isHoliday() && !day.getDate().getDayOfWeek().equals(DayOfWeek.SUNDAY)).count();

       // VERIFY
       // Use the new GetNext7DaySlotsQuery to retrieve the persisted slots
       Map<LocalDate, List<Slot>> weeklySlots = getNext7DaySlotsQuery.execute(branch.getBranchId(), LocalDate.now());
       
       assertThat(weeklySlots).as("Weekly slots map should not be empty").isNotEmpty();
       assertThat(weeklySlots.size()).as("Should generate slots should exclude today, sunday and public holidays days").isEqualTo(5-count);

       // 0. Verify Slot fields
       List<Slot> list = weeklySlots.values()
               .stream()
               .flatMap(List::stream).toList();

       for (Slot slot : list) {

           assertThat(slot.getDay()).isNotNull();


           Day day1 = days.stream().filter(day -> slot.getDay().equals(day.getDate())).findFirst().get();
           if(day1.isHoliday()) {

               assertThat(slot.getVersion()).isEqualTo(1);
               assertThat(slot.getStatus()).isEqualTo(SlotStatus.AVAILABLE);
               assertThat(slot.getBookingCount()).isEqualTo(0);
               assertThat(slot.getMaxBookingCapacity()).isEqualTo(3);
               assertThat(slot.getStartTime()).isBefore(slot.getEndTime());
               assertThat(slot.getDay()).isNotNull().isBeforeOrEqualTo(date.plusDays(7));
               assertThat(slot.getDuration())
                       .isEqualTo(Duration.between(slot.getStartTime(), slot.getEndTime()))
                       .isEqualTo(Duration.ofMinutes(30));
           }
           else if(day1.getDate().getDayOfWeek().equals(DayOfWeek.SATURDAY)) {

               assertThat(slot.getVersion()).isEqualTo(1);
               assertThat(slot.getStatus()).isEqualTo(SlotStatus.AVAILABLE);
               assertThat(slot.getBookingCount()).isEqualTo(0);
               assertThat(slot.getMaxBookingCapacity()).isEqualTo(1);
               assertThat(slot.getStartTime()).isBefore(slot.getEndTime());
               assertThat(slot.getDay()).isNotNull().isBeforeOrEqualTo(date.plusDays(7));
               assertThat(slot.getDuration())
                       .isEqualTo(Duration.between(slot.getStartTime(), slot.getEndTime()))
                       .isEqualTo(Duration.ofMinutes(30));
           }


       }

       // 1. Verify Holidays (Slots should be empty)
       List<LocalDate> holidays = weeklySlots.keySet()
               .stream()
               .filter(checkHolidayQuery::execute)
               .toList();

       for (LocalDate day : holidays) {
           List<Slot> holidaySlots = weeklySlots.get(day);
           assertThat(holidaySlots).as("Holiday slots should be empty for " + day).isEmpty();
       }

       // 2. Verify Week Days (Expected size: 9)
       List<LocalDate> weekDays = weeklySlots.keySet().stream()
               .filter(d -> !(checkHolidayQuery.execute(d) || isWeekend(d.getDayOfWeek())))
               .toList();

       for (LocalDate day : weekDays) {
           List<Slot> weekDaySlots = weeklySlots.get(day);
           assertThat(weekDaySlots).as("Week day slots should be generated for " + day).isNotEmpty();
           // Assuming default config yields 9 slots
           assertThat(weekDaySlots.size()).as("Week day slot count mismatch for " + day).isEqualTo(9);
       }

       // 3. Verify Saturday (Expected size: 5)
       var weekEnd= weeklySlots.keySet().stream()
               .filter(d -> d.getDayOfWeek().equals(DayOfWeek.SATURDAY))
               .findFirst();

       if (weekEnd.isPresent() ) {
           LocalDate day = weekEnd.get();
           var weekendSlots = weeklySlots.get(day);
           assertThat(weekendSlots).as("Weekend slots should be generated for " + day).isNotNull();
           // Assuming day after today and exclude  config yields 6 slots between 8am-13pm
           assertThat(weekendSlots.size()).as("Weekend slot count mismatch for " + day).isEqualTo(5);
       }
   }
}