package capitec.branch.appointment.slots.app;

import capitec.branch.appointment.slots.domain.SlotDayType;
import capitec.branch.appointment.slots.domain.Slot;
import capitec.branch.appointment.slots.domain.SlotStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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

       generateSlotsUseCase.createNext7DaySlots(null,0);

       // VERIFY
       // Use the new GetNext7DaySlotsQuery to retrieve the persisted slots
       Map<LocalDate, List<Slot>> weeklySlots = getNext7DaySlotsQuery.execute(branch.getBranchId(), LocalDate.now());
       
       assertThat(weeklySlots).as("Weekly slots map should not be empty").isNotEmpty();
       assertThat(weeklySlots.size()).as("Should generate slots for exactly 7 days").isEqualTo(7);

       // 0. Verify Slot fields
       List<Slot> list = weeklySlots.values()
               .stream()
               .flatMap(List::stream).toList();

       for (Slot slot : list) {

           assertThat(slot.getDay()).isNotNull();

           boolean isHoliday = checkHolidayQuery.execute(slot.getDay());
           boolean isWeekend = isWeekend(slot.getDay().getDayOfWeek());
           Map<SlotDayType, SlotProperties> dayTypeSlotPropertiesMap = branchSlotConfigs.branchConfigs().get(branch.getBranchId());

           SlotProperties slotProperties = (isHoliday) ?
                   dayTypeSlotPropertiesMap.get(SlotDayType.HOLIDAY) :
                   isWeekend ?
                   dayTypeSlotPropertiesMap.get(SlotDayType.WEEKEND):
                   dayTypeSlotPropertiesMap.get(SlotDayType.WEEK_DAYS);

           assertThat(slot.getVersion()).isEqualTo(1);
           assertThat(slot.getStatus()).isEqualTo(SlotStatus.AVAILABLE);
           assertThat(slot.getBookingCount()).isEqualTo(0);
           assertThat(slot.getMaxBookingCapacity()).isEqualTo(slotProperties.maxBookingCapacity());
           assertThat(slot.getStartTime()).isBefore(slot.getEndTime());
           assertThat(slot.getDay()).isNotNull().isBeforeOrEqualTo(LocalDate.now().plusDays(1).plusDays(7));
           assertThat(slot.getDuration())
                   .isEqualTo(Duration.between(slot.getStartTime(), slot.getEndTime()))
                   .isEqualTo(slotProperties.slotDuration());
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
       
       // 3. Verify Weekends (Expected size: 6)
       List<LocalDate> weekEnd= weeklySlots.keySet().stream()
               .filter(d -> isWeekend(d.getDayOfWeek()))
               .toList();

       for (var day : weekEnd ) {
           List<Slot> weekendSlots = weeklySlots.get(day);
           assertThat(weekendSlots).as("Weekend slots should be generated for " + day).isNotEmpty();
           // Assuming default config yields 6 slots
           assertThat(weekendSlots.size()).as("Weekend slot count mismatch for " + day).isEqualTo(6); 
       }
   }
}