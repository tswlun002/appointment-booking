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
import org.springframework.validation.annotation.Validated;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Slf4j
@UseCase
@Validated
@RequiredArgsConstructor
public class GenerateSlotsUseCase {
    private final static String COUNTRY= "South Africa";
    private final int SLOTS_DISTRIBUTION_FACTOR = 2;
    private final GetActiveBranchesForSlotGenerationPort activeBranchesForSlotGenerationPort;
    private final SlotService slotStorage;
    private static final int ROLLING_WINDOW_DAYS = 7;

    /**
     * Command to generate and save time slots for the next given days, by default 7 days.
     * @param  fromDate default to next day. The first day of slots that will be generated
     * @param nextDays default 7 days. The number of days of slots that will be generated starting from fromDate
     *
     */
    public void createNext7DaySlots(LocalDate fromDate, int nextDays) {

        LocalDate date = fromDate == null ? LocalDate.now().plusDays(1) : fromDate;

        Collection<BranchOperationTimesDetails> activeBranches = activeBranchesForSlotGenerationPort.execute(COUNTRY, date);

        int rolling_window = nextDays == 0 ? ROLLING_WINDOW_DAYS : nextDays;


        List<Slot> allSlots  = new ArrayList<>();
        for (var branch : activeBranches) {
            Map<LocalDate, List<Slot>> dayOfWeekListMap = generateTimeSlotsForRange(branch,date, rolling_window );
            if(!dayOfWeekListMap.isEmpty()) {
                List<Slot> list = dayOfWeekListMap.values().stream().flatMap(Collection::stream).toList();
                allSlots.addAll(list);
            }

        }
        if(allSlots.isEmpty()) {
            log.warn("Failed to generate slots, current date:{}", LocalDateTime.now());
            //throw new RuntimeException("Failed to generate slots, current date:" + LocalDateTime.now());
        }
        else slotStorage.save(allSlots);


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


            int availableCapacity = calculateAvailableCapacity(appointmentInfoDetails, operationTimesDetails);
            Duration slotDuration = appointmentInfoDetails.slotDuration();
            LocalTime closingTime = operationTimesDetails.closeAt();
            LocalTime openTime = operationTimesDetails.openAt();
            
            List<Slot> slots = new ArrayList<>();

            // Generate slots until the open time plus the slot duration exceeds closing time
            while (openTime.isBefore(closingTime) && openTime.plus(slotDuration).isBefore(closingTime.plusSeconds(1))) {

                if (slotGenerated < availableCapacity) {

                    LocalTime slotClosingTime = openTime.plus(slotDuration);
                    
                    Slot slot = new Slot(day, openTime, slotClosingTime, appointmentInfoDetails.maxBookingCapacity(),branch.branchId());
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

    private int calculateAvailableCapacity(AppointmentInfoDetails appointmentInfoDetails, OperationTimesDetails operationTimesDetails) {

        // 1. Calculate working duration in minutes
        LocalTime openTime = operationTimesDetails.openAt();
        LocalTime closingTime = operationTimesDetails.closeAt();

        // Calculate difference in hours/minutes (e.g., 17:00 - 08:00 = 9 hours)
        long workingMinutes = java.time.Duration.between(openTime, closingTime).toMinutes();

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