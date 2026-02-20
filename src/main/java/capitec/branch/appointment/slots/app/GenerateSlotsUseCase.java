package capitec.branch.appointment.slots.app;
import capitec.branch.appointment.slots.app.port.AppointmentInfoDetails;
import capitec.branch.appointment.slots.app.port.BranchOperationTimesDetails;
import capitec.branch.appointment.slots.app.port.GetActiveBranchesForSlotGenerationPort;
import capitec.branch.appointment.slots.app.port.OperationTimesDetails;
import capitec.branch.appointment.slots.domain.Slot;
import capitec.branch.appointment.slots.domain.SlotService;
import capitec.branch.appointment.utils.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * Use case for generating bookable appointment slots for branches.
 *
 * <p>This use case generates time slots for all active branches based on their
 * operation hours and appointment configuration. It is typically invoked by a
 * scheduler to create slots for upcoming days (rolling window).</p>
 *
 * <h2>Slot Generation Algorithm:</h2>
 * <ol>
 *   <li>Fetches all active branches with their operation times and appointment info</li>
 *   <li>For each branch and each day in the rolling window:
 *     <ul>
 *       <li>Checks if branch is open (has operation times and not closed)</li>
 *       <li>Checks if appointment info exists for that day type</li>
 *       <li>Calculates available capacity based on staff count and utilization factor</li>
 *       <li>Generates slots from opening time to closing time with configured duration</li>
 *     </ul>
 *   </li>
 *   <li>Persists all generated slots in bulk</li>
 * </ol>
 *
 * <h2>Capacity Calculation:</h2>
 * <pre>
 * workingMinutes = closingTime - openingTime
 * theoreticalSlotsPerStaff = workingMinutes / slotDuration
 * totalCapacity = staffCount × theoreticalSlotsPerStaff
 * availableCapacity = totalCapacity × utilizationFactor
 * </pre>
 *
 * <h2>Configuration:</h2>
 * <ul>
 *   <li><b>ROLLING_WINDOW_DAYS:</b> Default 7 days - how many days ahead to generate slots</li>
 *   <li><b>SLOTS_DISTRIBUTION_FACTOR:</b> 2 - spacing factor between slot start times</li>
 *   <li><b>COUNTRY:</b> "South Africa" - country for branch filtering</li>
 * </ul>
 *
 * <h2>Example:</h2>
 * <p>Branch 470010 configuration:</p>
 * <ul>
 *   <li>Opens: 08:00, Closes: 17:00 (9 hours = 540 minutes)</li>
 *   <li>Slot duration: 30 minutes</li>
 *   <li>Staff count: 3</li>
 *   <li>Utilization factor: 0.8</li>
 * </ul>
 * <p>Calculation:</p>
 * <pre>
 * theoreticalSlotsPerStaff = 540 / 30 = 18
 * totalCapacity = 3 × 18 = 54
 * availableCapacity = 54 × 0.8 = 43 slots
 * </pre>
 *
 * <h2>Error Handling:</h2>
 * <ul>
 *   <li>Branches without operation times are skipped with warning log</li>
 *   <li>Branches without appointment info are skipped with warning log</li>
 *   <li>Closed days are skipped</li>
 *   <li>If no slots are generated, a warning is logged (no exception thrown)</li>
 * </ul>
 *
 * @see Slot
 * @see SlotService
 * @see BranchOperationTimesDetails
 * @see AppointmentInfoDetails
 * @see GetActiveBranchesForSlotGenerationPort
 */
@Slf4j
@UseCase
@Validated
@RequiredArgsConstructor
public class GenerateSlotsUseCase {
    private final static String COUNTRY= "South Africa";
    @Value("${slot.duration.factor}")
    private   int SLOTS_DISTRIBUTION_FACTOR;
    private final GetActiveBranchesForSlotGenerationPort activeBranchesForSlotGenerationPort;
    private final SlotService slotStorage;
    private static final int ROLLING_WINDOW_DAYS = 7;

    /**
     * Command to generate and save time slots for the next given days, by default 7 days.
     * @param  fromDate default to the next day. The first day of slots that will be generated
     * @param nextDays default 7 days. The number of days of slots that will be generated starting from fromDate
     *
     */
    public void createNext7DaySlots(LocalDate fromDate, int nextDays) {
        createNext7DaySlots(Collections.emptySet(),fromDate, nextDays);
    }
    /**
     * Command to generate and save time slots for the next given days, by default 7 days.
     * @param  fromDate default to the next day. The first day of slots that will be generated
     * @param nextDays default 7 days. The number of days of slots that will be generated starting from fromDate
     * @param branches  branches to generate slots for. Branches must exist in the system. If not provide, generate for
     *                  branches registered in the system
     *
     */
    public void createNext7DaySlots(Set<String> branches,LocalDate fromDate, int nextDays) {
       try {
           LocalDate date = fromDate == null ? LocalDate.now().plusDays(1) : fromDate;

           Collection<BranchOperationTimesDetails> activeBranches = branches.isEmpty()? activeBranchesForSlotGenerationPort.execute(COUNTRY, date)
           :activeBranchesForSlotGenerationPort.execute(branches,COUNTRY, date);

           int rolling_window = nextDays == 0 ? ROLLING_WINDOW_DAYS : nextDays;


           List<Slot> allSlots = new ArrayList<>();
           for (var branch : activeBranches) {
               Map<LocalDate, List<Slot>> dayOfWeekListMap = generateTimeSlotsForRange(branch, date, rolling_window);
               if (!dayOfWeekListMap.isEmpty()) {
                   List<Slot> list = dayOfWeekListMap.values().stream().flatMap(Collection::stream).toList();
                   allSlots.addAll(list);
               }

           }
           if (allSlots.isEmpty()) {
               log.warn("Failed to generate slots, current date:{}", LocalDateTime.now());
               //throw new RuntimeException("Failed to generate slots, current date:" + LocalDateTime.now());
           } else slotStorage.save(allSlots);
       }catch (Exception e) {
           log.error("Failed to generate slots, current date:{}", fromDate);
           throw e;
       }


    }

    /**
     * Generate Time Slots for a given dateOfSlots range.
     * @param branch The branch of slots to generate
     * @param startDate The starting dateOfSlots.
     * @param days The bookingCount of days to generate slots for.
     * @return A map of generated slots grouped by dateOfSlots.
     */
    private Map<LocalDate, List<Slot>> generateTimeSlotsForRange(BranchOperationTimesDetails branch, LocalDate startDate, int days) {

        final Map<LocalDate, List<Slot>> weeklySlots = new HashMap<>();
        
        LocalDate day = startDate;

        Map<LocalDate, OperationTimesDetails> localDateOperationTimesDtoMap = branch.operationTimes();
        Map<LocalDate, AppointmentInfoDetails> AppointmentInfoDtoMap = branch.appointmentInfo();


        if (localDateOperationTimesDtoMap ==null || localDateOperationTimesDtoMap.isEmpty()) {
            log.error("Branch has no operation times found, branch:{}",branch);
            return  Collections.emptyMap();
        }

        if (AppointmentInfoDtoMap ==null || AppointmentInfoDtoMap.isEmpty()) {
            log.error("Branch has no appointment information found, branch:{}",branch.branchId());
            return  Collections.emptyMap();
        }


        for (int numDays = 0; numDays < days; numDays++) {
            log.debug("Creating slot for date {}", day.plusDays(numDays));
            int slotGenerated = 0;

            // Check if OperationTimes exist for this day
            OperationTimesDetails operationTimesDetails = localDateOperationTimesDtoMap.get(day);
            if (operationTimesDetails == null || operationTimesDetails.isClose()) {
                log.warn("Branch {} has operation times detected for day {}",branch.branchId(), day);
                day = day.plusDays(1);
                continue;
            }
            // Check if appointmentInfo exist for this day
            AppointmentInfoDetails appointmentInfoDetails = AppointmentInfoDtoMap.get(day);
            if (appointmentInfoDetails == null) {
                 log.warn("Branch {} has no appointment info found for day {}", branch.branchId(),day);
                day = day.plusDays(1);
                continue;
            }

            log.debug("Operation hours: {} ", operationTimesDetails);
            log.debug("Branch appointment info : {} ", appointmentInfoDetails);

            int availableCapacity = calculateAvailableCapacity(appointmentInfoDetails, operationTimesDetails);
            Duration slotDuration = appointmentInfoDetails.slotDuration();
            LocalTime closingTime = operationTimesDetails.closeAt();
            LocalTime openTime = operationTimesDetails.openAt();
            
            List<Slot> slots = new ArrayList<>();

            LocalDateTime openDateTime = day.atTime(openTime);
            LocalDateTime closeDateTime = day.atTime(closingTime);
            // Generate slots until the open time plus the slot duration exceeds closing time
            while (true) {

                if (!(openDateTime.isBefore(closeDateTime) && openDateTime.plus(slotDuration).isBefore(closeDateTime.plusSeconds(1)))) {
                    break;
                }

                if (slotGenerated < availableCapacity) {

                    var slotClosingTime = openDateTime.plus(slotDuration);
                    
                    Slot slot = new Slot(day, openDateTime.toLocalTime(), slotClosingTime.toLocalTime(), appointmentInfoDetails.maxBookingCapacity(),branch.branchId());
                    slots.add(slot);
                    slotGenerated++;
                }
                else {
                    log.debug("Capacity reached for {}, breaking loop to save time", day);
                    break;
                }
                
                // Move to the next slot time, respecting the distribution factor
                long minutesToAdd = slotDuration.toMinutes() * SLOTS_DISTRIBUTION_FACTOR;
                // FORCE the increment to be at least 1 minute to prevent infinite loops
                openDateTime = openDateTime.plusMinutes(Math.max(10, minutesToAdd));
            }

            weeklySlots.put(day, slots);
            day = day.plusDays(1);
        }
       log.debug("Generated slots:{}", weeklySlots);
        return weeklySlots;
    }

    private int calculateAvailableCapacity(AppointmentInfoDetails appointmentInfoDetails, OperationTimesDetails operationTimesDetails) {

        // 1. Calculate working duration in minutes
        LocalTime openTime = operationTimesDetails.openAt();
        LocalTime closingTime = operationTimesDetails.closeAt();

        // Calculate the difference in hours/minutes (e.g., 17:00-08:00 = 9 hours)
        long workingMinutes = Duration.between(openTime, closingTime).toMinutes();

        // 2. Calculate theoretical slots per staff
        long slotDurationMinutes = appointmentInfoDetails.slotDuration().toMinutes();
        var theoreticalSlotsPerStaff = workingMinutes / slotDurationMinutes;

        // 3. Calculate total theoretical capacity
        var totalCapacity = appointmentInfoDetails.staffCount() * theoreticalSlotsPerStaff;

        // 4. Calculate available capacity using utilization factor
        var availableCapacity = totalCapacity * appointmentInfoDetails.utilizationFactor();

        return (int) Math.round(availableCapacity);
    }
}