package capitec.branch.appointment.slots.app;

import capitec.branch.appointment.slots.domain.Slot;
import capitec.branch.appointment.slots.domain.SlotStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GetSlotQueriesTest extends SlotTestBase {

    @Autowired
    private GetDailySlotsQuery getDailySlotsQuery;
    @Autowired
    private GetNext7DaySlotsQuery getNext7DaySlotsQuery;
    private final LocalDate TODAY = LocalDate.now();
    private final LocalDate TOMORROW = TODAY.plusDays(1);
    private final LocalDate DAY_AFTER = TODAY.plusDays(2);
    private final int  MAX_BOOKING_CAPACITY = 1;
    
    // Example Slots for arrangement
    private final Slot slot1 = new Slot(TODAY, LocalTime.of(9, 0), LocalTime.of(9, 30), MAX_BOOKING_CAPACITY, branchId);
    private final Slot slot2 = new Slot(TODAY, LocalTime.of(9, 30), LocalTime.of(10, 0), MAX_BOOKING_CAPACITY, branchId);
    private final Slot slot3 = new Slot(TOMORROW, LocalTime.of(9, 0), LocalTime.of(9, 30), MAX_BOOKING_CAPACITY, branchId);
    private final Slot slot4 = new Slot(TOMORROW, LocalTime.of(9, 30), LocalTime.of(10, 0), MAX_BOOKING_CAPACITY, branchId);
    private final Slot slot5 = new Slot(DAY_AFTER, LocalTime.of(8, 0), LocalTime.of(8, 30), MAX_BOOKING_CAPACITY, branchId);
    
    
    @BeforeEach
    void setupTestData() {
        slotService.save(List.of(slot1, slot2, slot3, slot4, slot5));

    }


    @Test
    void testGetDailySlotsQuery_RetrievesSlotsForSpecificDay() {
        // ARRANGE: (Assuming SlotStorage is set up to return slot1 and slot2 for TODAY)

        List<Slot> dailySlots = getDailySlotsQuery.execute(branchId,TODAY);
        
        // ASSERT
        assertThat(dailySlots).as("Should retrieve exactly 2 slots for today").hasSize(2);
        assertThat(dailySlots).containsExactlyInAnyOrder(slot1, slot2);
    }

    @Test
    void testGetNext7DaySlotsQuery_RetrievesAndGroupsByDay() {
        // ARRANGE: (Assuming SlotStorage is set up to return all 5 slots for the next 7 days)

        Map<LocalDate, List<Slot>> weeklySlotsMap = getNext7DaySlotsQuery.execute(branchId,TODAY);
        
        // ASSERT
        assertThat(weeklySlotsMap).as("Map should contain three unique days").hasSize(3);
        assertThat(weeklySlotsMap).containsKey(TODAY).containsKey(TOMORROW).containsKey(DAY_AFTER);
        
        assertThat(weeklySlotsMap.get(TODAY)).as("Slots for TODAY should be correct").hasSize(2).containsExactlyInAnyOrder(slot1, slot2);
        assertThat(weeklySlotsMap.get(TOMORROW)).as("Slots for TOMORROW should be correct").hasSize(2).containsExactlyInAnyOrder(slot3, slot4);
    }
    
    @Test
    void testGetNext7DaySlotsQuery_WithStatusFilter() {

        Map<LocalDate, List<Slot>> filteredSlotsMap = getNext7DaySlotsQuery.execute(branchId,TODAY, SlotStatus.AVAILABLE);

        // ASSERT
        assertThat(filteredSlotsMap).as("Map should contain 3 days with available slots").hasSize(3);
        assertThat(filteredSlotsMap.get(TODAY)).as("TODAY should have 2 available slot").hasSize(2).contains(slot1);
        assertThat(filteredSlotsMap.get(TOMORROW)).as("TOMORROW should have 2 available slot").hasSize(2).contains(slot3);
        assertThat(filteredSlotsMap.get(DAY_AFTER)).as("DAY_AFTER should have 1 available slot").hasSize(1).contains(slot5);
    }
}