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

    @Autowired
    protected SlotService slotService;
    protected final String branchId = "BR001";


    @AfterEach
    public void cleanupSlots() {

        List<Slot> slots = slotService.getNext7DaySlots(branchId,LocalDate.now());

        for (Slot slot : slots) {
            slotService.cleanUpSlot(slot.getId());
        }
    }
}