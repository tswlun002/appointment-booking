package capitec.branch.appointment.slots.app;

import capitec.branch.appointment.slots.domain.Day;
import capitec.branch.appointment.slots.domain.SlotDayType;
import capitec.branch.appointment.slots.domain.Slot;
import capitec.branch.appointment.slots.domain.SlotService;
import capitec.branch.appointment.utils.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@UseCase
@Validated
@RequiredArgsConstructor
public class GenerateSlotsUseCase {

    private final int SLOTS_DISTRIBUTION_FACTOR = 2;
    private final BranchSlotConfigs branchSlotConfigs;
    private final SlotService slotStorage;
    private final CheckHolidayQuery checkHolidayQuery;
    private final CalculateAvailableCapacityService calculateAvailableCapacityService;

    private static final int ROLLING_WINDOW_DAYS = 7;

    /**
     * Command to generate and save time slots for the next given days, by default 7 days.
     * @param  fromDate default to next day. The first day of slots that will be generated
     * @param nextDays default 7 days. The number of days of slots that will be generated starting from fromDate
     *
     */
    public void createNext7DaySlots(LocalDate fromDate, int nextDays) {
        int rolling_window = nextDays == 0 ? ROLLING_WINDOW_DAYS : nextDays;
        LocalDate date = fromDate == null ? LocalDate.now().plusDays(1) : fromDate;

        Set<String> strings = branchSlotConfigs
                .branchConfigs()
                .keySet()
                .stream().
                filter(s->!s.equals(BranchSlotConfigs.DEFAULT_CONFIG_KEY))
                .collect(Collectors.toSet());
        List<Slot> allSlots  = new ArrayList<>();
        for (String branch : strings) {
            Map<LocalDate, List<Slot>> dayOfWeekListMap = generateTimeSlotsForRange(branch,date, rolling_window );
            List<Slot> list = dayOfWeekListMap.values().stream().flatMap(Collection::stream).toList();
            allSlots.addAll(list);
        }
        slotStorage.save(allSlots);


    }

    /**
     * Generate Time Slots for a given dateOfSlots range.
     * @param branchId The id of the branch of slot to generate
     * @param startDate The starting dateOfSlots.
     * @param days The bookingCount of days to generate slots for.
     * @return A map of generated slots grouped by dateOfSlots.
     */
    private Map<LocalDate, List<Slot>> generateTimeSlotsForRange(String branchId,LocalDate startDate, int days) {

        final Map<LocalDate, List<Slot>> weeklySlots = new HashMap<>();
        
        LocalDate day = startDate;
        for (int numDays = 0; numDays < days; numDays++) {
            
            int slotGenerated = 0;
            
            SlotDayType slotDayType = checkHolidayQuery.execute(day) ? SlotDayType.HOLIDAY :
                              Day.isWeekend(day.getDayOfWeek()) ? SlotDayType.WEEKEND : SlotDayType.WEEK_DAYS;

            var slotProperties = branchSlotConfigs
                    .branchConfigs()
                    .get(branchId)
                    .get(slotDayType);
            
            // Check if branchConfigs exist for this day type
            if (slotProperties == null) {
                log.warn("No slot branchConfigs configured for SlotDayType: {}. Skipping slot generation for {}.", slotDayType, day);
                day = day.plusDays(1);
                continue; 
            }

            Duration slotDuration = slotProperties.slotDuration();
            int availableCapacity = calculateAvailableCapacityService.execute(branchId, slotDayType);
            LocalTime closingTime = slotProperties.closingTime();
            LocalTime openTime = slotProperties.openTime();
            
            List<Slot> slots = new ArrayList<>();

            // Generate slots until the open time plus the slot duration exceeds closing time
            while (openTime.isBefore(closingTime) && openTime.plus(slotDuration).isBefore(closingTime.plusSeconds(1))) {

                if (slotGenerated < availableCapacity) {

                    LocalTime slotClosingTime = openTime.plus(slotDuration);
                    
                    Slot slot = new Slot(day, openTime, slotClosingTime, slotProperties.maxBookingCapacity(),branchId);
                    slots.add(slot);
                    slotGenerated++;
                }
                
                // Move to the next slot time, respecting the distribution factor
                openTime = openTime.plusMinutes(slotDuration.toMinutes() * SLOTS_DISTRIBUTION_FACTOR);
            }

            weeklySlots.put(day, slots);
            day = day.plusDays(1);
        }

        return weeklySlots;
    }
}