package capitec.branch.appointment.slots.app;

import capitec.branch.appointment.AppointmentBookingApplicationTests;
import capitec.branch.appointment.branch.domain.Branch;
import capitec.branch.appointment.branch.domain.BranchService;
import capitec.branch.appointment.branch.domain.appointmentinfo.BranchAppointmentInfo;
import capitec.branch.appointment.branch.domain.appointmentinfo.DayType;
import capitec.branch.appointment.branch.infrastructure.dao.BranchDaoImpl;
import capitec.branch.appointment.sharekernel.day.app.GetDateOfNextDaysQuery;
import capitec.branch.appointment.sharekernel.day.domain.Day;
import capitec.branch.appointment.location.infrastructure.api.CapitecBranchLocationFetcher;
import capitec.branch.appointment.slots.app.port.SlotCleanupPort;
import capitec.branch.appointment.slots.app.port.SlotQueryPort;
import capitec.branch.appointment.slots.domain.Slot;
import capitec.branch.appointment.slots.domain.SlotService;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Base class for all Slot Use Case tests.
 */
abstract class SlotTestBase extends AppointmentBookingApplicationTests {

    @Autowired
    protected SlotService slotService;
    @Autowired
    protected SlotQueryPort slotQueryPort;
    @Autowired
    protected SlotCleanupPort slotCleanupPort;
    @Autowired
    private BranchService branchService;
    protected  Branch branch;
    protected  List<Branch> branches = new ArrayList<>();
    @Autowired
    @Qualifier("branchLocationCacheManager")
    protected CacheManager cacheManagerBranchLocationService;
    @Autowired
    @Qualifier("branchCacheManager")
    protected CacheManager cacheManagerBranches;
    @Autowired
    protected CircuitBreakerRegistry circuitBreakerRegistry;
    private WireMock capitecWireMock;
    @Autowired
    protected GetDateOfNextDaysQuery getDateOfNextDaysQuery;

    @BeforeEach
    public void setUp()  {
        setUpBranch();
    }
    @AfterEach
    public void cleanupSlots() {

        for (Branch branch : branches) {
            List<Slot> slots = slotQueryPort.findByBranchFromDate(branch.getBranchId(), LocalDate.now().plusDays(1));

            for (Slot slot : slots) {
                slotCleanupPort.deleteSlot(slot.getId());
            }
        }

        deleteBranches();
    }

    protected void setUpBranch() {


       // var nonDefault = branchSlotConfigs.branchConfigs().keySet().stream().filter(s->!s.equals("default")).findFirst().get();

        //var defaultBranch = branchSlotConfigs.branchUseDefaultConfigs().getFirst();

       capitecWireMock = new WireMock(
               wiremockContainer.getHost(),
               wiremockContainer.getFirstMappedPort()
       );

        // Reset any previous stubs
        capitecWireMock.resetMappings();
        stubCapitecApiSuccess(capitecWireMock, capitecApiBranchResponse());
        // Reset circuit breaker state before each test
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("branchLocatorCircuitBreaker");
        cb.reset();

        // Clear caches
        clearCaches();
        for (var branchId: new String[]{ "SAS293200", "SAS29300"}) {

            branch = new Branch(branchId,"Capitec Head Office");
            LocalDate now = LocalDate.now();
            Set<Day> execute = getDateOfNextDaysQuery.execute(now.getDayOfWeek(), now.plusDays(6).getDayOfWeek());

            for(var day : execute) {

                if(day.isHoliday())continue;

                else if(day.isWeekday()){

                    DayType day1 = DayType.valueOf(day.getDate().getDayOfWeek().name());
                    var branchAppointmentInfo =
                            new BranchAppointmentInfo(
                                    Duration.ofMinutes(30),
                                    0.6,
                                    6,
                                    day1,
                                    3
                            );
                    branch.updateAppointmentInfo(day1, branchAppointmentInfo, LocalTime.of(8,0), LocalTime.of(17,0));
                }
                else if(day.getDate().getDayOfWeek().equals(DayOfWeek.SATURDAY)){
                    var branchAppointmentInfo =
                            new BranchAppointmentInfo(
                                    Duration.ofMinutes(30),
                                    0.3,
                                    4,
                                    DayType.SATURDAY,
                                    1
                            );
                    branch.updateAppointmentInfo(DayType.SATURDAY, branchAppointmentInfo, LocalTime.of(8,0), LocalTime.of(17,0));

                }

            }



            branches.add(branchService.add(branch));
        }
       branch = branches.getFirst();


    }
    private void clearCaches() {
        var cache = new Cache[]{
                cacheManagerBranchLocationService.getCache(CapitecBranchLocationFetcher.BRANCH_LOCATIONS_BY_COORDINATES_CACHE),
                cacheManagerBranchLocationService.getCache(CapitecBranchLocationFetcher.BRANCH_LOCATIONS_BY_AREA_CACHE),
                cacheManagerBranches.getCache(BranchDaoImpl.CACHE_NAME)
        };
        for (Cache cache1 : cache) {
            if(cache1!=null) {
                cache1.clear();
            }
        }
    }

    protected void deleteBranches() {
        capitecWireMock.resetMappings();
       for (Branch branch : branches) {
           branchService.delete(branch.getBranchId());
       }

    }
}