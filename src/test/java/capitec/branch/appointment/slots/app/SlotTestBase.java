package capitec.branch.appointment.slots.app;

import capitec.branch.appointment.AppointmentBookingApplicationTests;
import capitec.branch.appointment.slots.domain.Slot;
import capitec.branch.appointment.slots.domain.SlotService;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;

/**
 * Base class for all Slot Use Case tests.
 */
abstract class SlotTestBase extends AppointmentBookingApplicationTests {

    // Inject all required services/use cases for testing and cleanup
    @Autowired
    protected SlotService slotStorage;
    
    // We mock the HolidayClient in the base test if we don't want real API calls, 
    // but the original test suggests using WireMock, so we'll leave it out here 
    // but note it's important for isolation.

    @AfterEach
    public void cleanupSlots() {

        List<Slot> slots = slotStorage.next7DaySlots(LocalDate.now());

        for (Slot slot : slots) {
            slotStorage.cleanUpSlot(slot.getNumber());
        }
    }
}