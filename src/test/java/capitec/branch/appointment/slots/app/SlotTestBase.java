package capitec.branch.appointment.slots.app;

import capitec.branch.appointment.AppointmentBookingApplicationTests;
import capitec.branch.appointment.branch.domain.Branch;
import capitec.branch.appointment.branch.domain.BranchService;
import capitec.branch.appointment.branch.domain.address.Address;
import capitec.branch.appointment.day.app.CheckHolidayQuery;
import capitec.branch.appointment.day.app.GetDayTypeQuery;
import capitec.branch.appointment.day.domain.DayType;
import capitec.branch.appointment.day.domain.HolidayClient;
import capitec.branch.appointment.slots.domain.Slot;
import capitec.branch.appointment.slots.domain.SlotService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static capitec.branch.appointment.day.domain.Day.isWeekend;

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
    @Autowired
    GetDayTypeQuery getDayTypeQuery;

    @BeforeEach
    public void setUp()  {
        setUpBranch();
    }
    @AfterEach
    public void cleanupSlots() {

        for (Branch branch : branches) {
            List<Slot> slots = slotService.getNext7DaySlots(branch.getBranchId(),LocalDate.now().plusDays(1));

            for (Slot slot : slots) {
                slotService.cleanUpSlot(slot.getId());
            }
        }

        deleteBranches();
    }

    protected void setUpBranch() {

        var address1 = "1;Main Street;Rosebank;Johannesburg;Gauteng;2196;South Africa";
        var address2 = "2;Main Street;Soweto;Johannesburg;Gauteng;2196;South Africa";

        var nonDefault = branchSlotConfigs.branchConfigs().keySet().stream().filter(s->!s.equals("default")).findFirst().get();

        var defaultBranch = branchSlotConfigs.branchUseDefaultConfigs().getFirst();


        for (var i=0; i<2; i++) {

            String branchId = i==0?nonDefault:defaultBranch;
            String[] address1Info = i==0 ? address1.split(";"): address2.split(";");

            var day = LocalDate.now().plusDays(1);
            var dayTye  = getDayTypeQuery.execute(day);

            var propertiesMap = branchSlotConfigs.branchConfigs().get(branchId).get(dayTye);

            Address address = new Address(address1Info[0], address1Info[1], address1Info[2], address1Info[3], address1Info[4], Integer.parseInt(address1Info[5]), address1Info[6]);
            branch = new Branch(branchId,propertiesMap.openTime(),propertiesMap.closingTime(), address);

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