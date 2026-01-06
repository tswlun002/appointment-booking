package capitec.branch.appointment.slots.app;

import capitec.branch.appointment.day.domain.DayType;
import capitec.branch.appointment.utils.UseCase;
import lombok.RequiredArgsConstructor;

import java.time.LocalTime;

@UseCase
@RequiredArgsConstructor
class CalculateAvailableCapacityService {

    private final MapSlotProperties mapSlotProperties;

    /**
     * Calculates the available slot capacity for a given DayType based on staff count,
     * working hours, slot duration, and utilization factor.
     */
    public int execute(DayType dayType) {
        SlotProperties slotProperties = mapSlotProperties.slotProperties().get(dayType);
        
        // 1. Calculate working duration in minutes
        LocalTime openTime = slotProperties.openTime();
        LocalTime closingTime = slotProperties.closingTime();
        
        // Calculate difference in hours/minutes (e.g., 17:00 - 08:00 = 9 hours)
        long workingMinutes = java.time.Duration.between(openTime, closingTime).toMinutes();

        // 2. Calculate theoretical slots per staff
        long slotDurationMinutes = slotProperties.slotDuration().toMinutes();
        var theoreticalSlotsPerStaff = workingMinutes / slotDurationMinutes;

        // 3. Calculate total theoretical capacity
        var totalCapacity = slotProperties.staffCount() * theoreticalSlotsPerStaff;

        // 4. Calculate available capacity using utilization factor
        var availableCapacity = totalCapacity * slotProperties.utilizationFactor();

        return (int) Math.round(availableCapacity);
    }
}