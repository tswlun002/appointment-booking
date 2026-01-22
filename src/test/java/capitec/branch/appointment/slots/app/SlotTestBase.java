package capitec.branch.appointment.slots.app;

import capitec.branch.appointment.AppointmentBookingApplicationTests;
import capitec.branch.appointment.branch.domain.Branch;
import capitec.branch.appointment.branch.domain.BranchService;
import capitec.branch.appointment.slots.domain.Slot;
import capitec.branch.appointment.slots.domain.SlotService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for all Slot Use Case tests.
 */
abstract class SlotTestBase extends AppointmentBookingApplicationTests {

    @Autowired
    protected SlotService slotService;
    @Autowired
    private BranchService branchService;
    protected  Branch branch;
    protected  List<Branch> branches = new ArrayList<>();
    @Autowired
    protected BranchSlotConfigs branchSlotConfigs;

    @BeforeEach
    public void setUp()  {
        setUpBranch();
    }
    @AfterEach
    public void cleanupSlots() {

        for (Branch branch : branches) {
            List<Slot> slots = slotService.getSlots(branch.getBranchId(),LocalDate.now().plusDays(1));

            for (Slot slot : slots) {
                slotService.cleanUpSlot(slot.getId());
            }
        }

        deleteBranches();
    }

    protected void setUpBranch() {


        var nonDefault = branchSlotConfigs.branchConfigs().keySet().stream().filter(s->!s.equals("default")).findFirst().get();

        var defaultBranch = branchSlotConfigs.branchUseDefaultConfigs().getFirst();


        for (var i=0; i<2; i++) {

            String branchId = i==0?nonDefault:defaultBranch;

            branch = new Branch(branchId);

            branches.add(branchService.add(branch));
        }
       branch = branches.getFirst();




    }

    protected void deleteBranches() {
       for (Branch branch : branches) {
           branchService.delete(branch.getBranchId());
       }

    }
}