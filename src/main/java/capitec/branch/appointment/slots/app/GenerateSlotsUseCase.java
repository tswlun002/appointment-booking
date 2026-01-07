package capitec.branch.appointment.slots.app;

import capitec.branch.appointment.day.app.CheckHolidayQuery;
import capitec.branch.appointment.day.domain.Day;
import capitec.branch.appointment.day.domain.DayType;
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

    /**
     * Command to generate and save time slots for the next 7 days.
     */
    public void createNext7DaySlots() {

        Set<String> strings = branchSlotConfigs
                .branchConfigs()
                .keySet()
                .stream().
                filter(s->!s.equals(BranchSlotConfigs.DEFAULT_CONFIG_KEY))
                .collect(Collectors.toSet());

        for (String branch : strings) {
            Map<LocalDate, List<Slot>> dayOfWeekListMap = generateTimeSlotsForRange(branch,LocalDate.now(), 7);
            List<Slot> list = dayOfWeekListMap.values().stream().flatMap(Collection::stream).toList();
            slotStorage.save(list);
        }
    }

    /**
     * Generate Time Slots for a given date range.
     * @param branchId The id of the branch of slot to generate
     * @param startDate The starting date.
     * @param days The number of days to generate slots for.
     * @return A map of generated slots grouped by date.
     */
    private Map<LocalDate, List<Slot>> generateTimeSlotsForRange(String branchId,LocalDate startDate, int days) {

        final Map<LocalDate, List<Slot>> weeklySlots = new HashMap<>();
        
        LocalDate day = startDate;
        for (int numDays = 0; numDays < days; numDays++) {
            
            int slotGenerated = 0;
            
            DayType dayType = checkHolidayQuery.execute(day) ? DayType.HOLIDAY : 
                              Day.isWeekend(day.getDayOfWeek()) ? DayType.WEEKEND : DayType.WEEK_DAYS;

            var slotProperties = branchSlotConfigs
                    .branchConfigs()
                    .get(branchId)
                    .get(dayType);
            
            // Check if branchConfigs exist for this day type
            if (slotProperties == null) {
                log.warn("No slot branchConfigs configured for DayType: {}. Skipping slot generation for {}.", dayType, day);
                day = day.plusDays(1);
                continue; 
            }

            Duration slotDuration = slotProperties.slotDuration();
            int availableCapacity = calculateAvailableCapacityService.execute(branchId,dayType);
            LocalTime closingTime = slotProperties.closingTime();
            LocalTime openTime = slotProperties.openTime();
            
            List<Slot> slots = new ArrayList<>();

            // Generate slots until the open time plus the slot duration exceeds closing time
            while (openTime.isBefore(closingTime) && openTime.plus(slotDuration).isBefore(closingTime.plusSeconds(1))) {

                if (slotGenerated < availableCapacity) {

                    LocalTime slotClosingTime = openTime.plus(slotDuration);
                    
                    Slot slot = new Slot(day, openTime, slotClosingTime, slotGenerated,branchId);
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