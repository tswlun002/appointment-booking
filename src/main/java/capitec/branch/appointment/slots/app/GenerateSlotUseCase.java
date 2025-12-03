package capitec.branch.appointment.slots.app;

import capitec.branch.appointment.slots.domain.DayType;
import capitec.branch.appointment.slots.domain.Holiday;
import capitec.branch.appointment.slots.domain.Slot;
import capitec.branch.appointment.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;


@UseCase
@RequiredArgsConstructor
@Validated
public class GenerateSlotUseCase {

    private final  static String COUNTRY_CODE ="ZA";
    private final static int CURRENT_YEAR = LocalDateTime.now().getYear();
    private final int SLOTS_DISTRIBUTION_FACTOR = 2;
    private final MapSlotProperties mapSlotProperties;
    private final HolidayClient holidayClient;
    private final SlotStorage slotStorage;


    public List<Slot> dailySlot(LocalDate day) {
        return slotStorage.dailySlot(day);
    }

    public Map<LocalDate, List<Slot>> next7DaySlots(LocalDate date) {
        List<Slot> slots = slotStorage.next7DaySlots(date);
        return slots.stream().collect(Collectors.groupingBy(Slot::getDay));
    }

    public Map<LocalDate, List<Slot>> next7DaySlots(LocalDate date, boolean status) {
        List<Slot> slots = slotStorage.next7DaySlots(date, status);
        return slots.stream().collect(Collectors.groupingBy(Slot::getDay));
    }

    public  void createNext7DaySlots() {
        Map<LocalDate, List<Slot>> dayOfWeekListMap = generateTimeSlots();
        List<Slot> list = dayOfWeekListMap.values().stream().flatMap(Collection::stream).toList();
        slotStorage.save(list);
    }

    /**
     * Generate Time Slots
     * For each day:
     *   current_time = branch_opening_time (08:00)
     *   end_time = branch_closing_time (17:00)
     *
     *   while current_time + slot_duration <= end_time:
     *     if slots_generated < available_capacity:
     *       create_slot(date, current_time, status=AVAILABLE)
     *       slots_generated++
     *
     *     current_time += slot_duration * Distribution_factor
     */
    private Map<LocalDate, List<Slot>>   generateTimeSlots(){

         final Map<LocalDate, List<Slot>>  weeklySlots = new HashMap<>();
         

        var numDays  =0;
        var day = LocalDate.now();
        do {

            var slotGenerated  =0;

            List<Slot> slots = weeklySlots.get(day);

            var dayType = isHoliday(day)?DayType.HOLIDAY:isWeekend(day.getDayOfWeek())? DayType.WEEKEND:DayType.WEEK_DAYS;

            SlotProperties slotProperties = mapSlotProperties.slotProperties().get(dayType);

            Duration slotDuration = slotProperties.slotDuration();

            var availableCapacity = getAvailableCapacity(dayType);
            LocalTime closingTime = slotProperties.closingTime();
            LocalTime openTime = slotProperties.openTime();

            while (openTime.isBefore(closingTime)) {

                if (slotGenerated < availableCapacity) {

                    int minutes = Math.toIntExact(openTime.getMinute() + slotDuration.toMinutes());

                    var slotClosingTime = LocalTime.of(openTime.getHour(),0).plusMinutes(minutes);

                    Slot slot = new Slot(day,openTime, slotClosingTime, slotGenerated);

                    if( slots == null || slots.isEmpty()) {

                        slots = new ArrayList<>();
                        slots.add(slot);
                    }
                    else{
                        slots.add(slot);
                    }

                    weeklySlots.put(day,slots);
                    slotGenerated++;
                }
                openTime = openTime.plusMinutes(slotDuration.toMinutes()*SLOTS_DISTRIBUTION_FACTOR);
            }

            day = day.plusDays(1);
            numDays++;

        }while (numDays <7 );

        return  weeklySlots;
    }

    /**
     * For each day:
     *   staff_count = Number of staff scheduled that day
     *   working_hours = Branch hours (e.g., 9 hours = 08:00-17:00)
     *   slot_duration = 30 minutes (configurable by appointment type)
     *
     *   theoretical_slots = (working_hours × 60) / slot_duration
     *   Example: (9 × 60) / 30 = 18 slots per staff
     *
     *   total_capacity = staff_count × theoretical_slots
     *   Example: 5 staff × 18 = 90 slots
     *
     *   available_capacity = total_capacity × utilization_factor
     *   Example: 90 × 0.8 = 72 slots (leave 18 for walk-ins)
     * @return
     */
    private   int getAvailableCapacity(DayType dayType) {

        SlotProperties slotProperties = mapSlotProperties.slotProperties().get(dayType);
        LocalTime localTime = slotProperties.openTime();
        LocalTime localTime1 = slotProperties.closingTime();
        int i = localTime1.getHour() - localTime.getHour();
        int i1 = localTime1.getMinute() - localTime.getMinute();
        LocalTime localTime2 = LocalTime.of(i, i1);

        int workingHours =   localTime2.getHour();;
        var  theoreticalSlots  = (workingHours*60)/slotProperties.slotDuration().toMinutes();

        var totalCapacity =  slotProperties.staffCount() * theoreticalSlots;

        var availableCapacity = totalCapacity * slotProperties.utilizationFactor();

        return Math.toIntExact(Math.round(availableCapacity));
    }


    public static boolean isWeekend(DayOfWeek date) {

        return date== DayOfWeek.SATURDAY ||
                date == DayOfWeek.SUNDAY;
    }
    public  boolean isHoliday(LocalDate date) {
        Set<Holiday> holidays = holidayClient.getHolidays(COUNTRY_CODE, CURRENT_YEAR);

        return holidays.stream()
                .map(Holiday::date)
                .anyMatch(date::equals);
    }
}
