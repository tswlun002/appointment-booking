package capitec.branch.appointment.slots.app;

import capitec.branch.appointment.slots.domain.Slot;
import capitec.branch.appointment.slots.domain.SlotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GetSlotQueriesTest extends SlotTestBase {

    @Autowired
    private GetDailySlotsQuery getDailySlotsQuery;
    @Autowired
    private GetNext7DaySlotsQuery getNext7DaySlotsQuery;

    
    // Note: Since SlotStorage is often a repository/DAO, 
    // it's best to mock it here if we want to isolate the Use Case logic (mapping, grouping).
    // For simplicity in this example, we rely on the injected SlotStorage being a real repository.

    private final LocalDate TODAY = LocalDate.now();
    private final LocalDate TOMORROW = TODAY.plusDays(1);
    private final LocalDate DAY_AFTER = TODAY.plusDays(2);
    
    // Example Slots for arrangement
    private final Slot slot1 = new Slot(TODAY, LocalTime.of(9, 0), LocalTime.of(9, 30), 0);
    private final Slot slot2 = new Slot(TODAY, LocalTime.of(9, 30), LocalTime.of(10, 0), 1);
    private final Slot slot3 = new Slot(TOMORROW, LocalTime.of(9, 0), LocalTime.of(9, 30), 0);
    private final Slot slot4 = new Slot(TOMORROW, LocalTime.of(9, 30), LocalTime.of(10, 0), 1);
    private final Slot slot5 = new Slot(DAY_AFTER, LocalTime.of(8, 0), LocalTime.of(8, 30), 0);
    
    
    @BeforeEach
    void setupTestData() {
        // In a real integration test, you'd insert these via the repository/storage.
        // Since we don't have the explicit insert code, we will assume the storage
        // has a way to hold/provide this data for the test run.
        // For a true unit test, we'd mock the SlotStorage.
        
        // As this is a test split, we'll assume the SlotStorage is capable of providing this data
        // when called by the queries.
        slotStorage.save(List.of(slot1, slot2, slot3, slot4, slot5));

    }


    @Test
    void testGetDailySlotsQuery_RetrievesSlotsForSpecificDay() {
        // ARRANGE: (Assuming SlotStorage is set up to return slot1 and slot2 for TODAY)
        
        // ACT
        List<Slot> dailySlots = getDailySlotsQuery.execute(TODAY);
        
        // ASSERT
        assertThat(dailySlots).as("Should retrieve exactly 2 slots for today").hasSize(2);
        assertThat(dailySlots).containsExactlyInAnyOrder(slot1, slot2);
    }

    @Test
    void testGetNext7DaySlotsQuery_RetrievesAndGroupsByDay() {
        // ARRANGE: (Assuming SlotStorage is set up to return all 5 slots for the next 7 days)
        
        // ACT
        Map<LocalDate, List<Slot>> weeklySlotsMap = getNext7DaySlotsQuery.execute(TODAY);
        
        // ASSERT
        assertThat(weeklySlotsMap).as("Map should contain three unique days").hasSize(3);
        assertThat(weeklySlotsMap).containsKey(TODAY).containsKey(TOMORROW).containsKey(DAY_AFTER);
        
        assertThat(weeklySlotsMap.get(TODAY)).as("Slots for TODAY should be correct").hasSize(2).containsExactlyInAnyOrder(slot1, slot2);
        assertThat(weeklySlotsMap.get(TOMORROW)).as("Slots for TOMORROW should be correct").hasSize(2).containsExactlyInAnyOrder(slot3, slot4);
    }
    
 /*   @Test
    void testGetNext7DaySlotsQuery_WithStatusFilter() {
        // ARRANGE: Assume slot1 and slot3 are marked as AVAILABLE (status=false)

        // ACT
        Map<LocalDate, List<Slot>> filteredSlotsMap = getNext7DaySlotsQuery.execute(TODAY, false);

        // ASSERT
        // This test requires SlotStorage to be fully functional or mocked to show the filter works.
        // Assuming the repository returns only slot1 and slot3 when status=true
        assertThat(filteredSlotsMap).as("Map should contain two days with available slots").hasSize(2);
        assertThat(filteredSlotsMap.get(TODAY)).as("TODAY should have 1 available slot").hasSize(1).contains(slot1);
        assertThat(filteredSlotsMap.get(TOMORROW)).as("TOMORROW should have 1 available slot").hasSize(1).contains(slot3);
        assertThat(filteredSlotsMap).doesNotContainKey(DAY_AFTER);
    }*/
}