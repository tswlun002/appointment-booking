package capitec.branch.appointment.appointment.app;

import capitec.branch.appointment.AppointmentBookingApplicationTests;
import capitec.branch.appointment.appointment.domain.Appointment;
import capitec.branch.appointment.appointment.domain.AppointmentService;
import capitec.branch.appointment.branch.app.*;
import capitec.branch.appointment.branch.domain.Branch;
import capitec.branch.appointment.branch.domain.appointmentinfo.DayType;
import capitec.branch.appointment.branch.infrastructure.dao.BranchDaoImpl;
import capitec.branch.appointment.sharekernel.day.app.GetDateOfNextDaysQuery;
import capitec.branch.appointment.sharekernel.day.domain.Day;
import capitec.branch.appointment.kafka.domain.EventValue;
import capitec.branch.appointment.location.infrastructure.api.CapitecBranchLocationFetcher;
import capitec.branch.appointment.slots.app.GetNext7DaySlotsQuery;
import capitec.branch.appointment.slots.app.SlotGeneratorScheduler;
import capitec.branch.appointment.sharekernel.event.metadata.AppointmentMetadata;
import capitec.branch.appointment.keycloak.domain.KeycloakService;
import capitec.branch.appointment.role.domain.FetchRoleByNameService;
import capitec.branch.appointment.slots.domain.Slot;
import capitec.branch.appointment.slots.domain.SlotService;
import capitec.branch.appointment.staff.domain.Staff;
import capitec.branch.appointment.staff.domain.StaffService;
import capitec.branch.appointment.staff.domain.StaffStatus;
import capitec.branch.appointment.user.domain.UserRoleService;
import capitec.branch.appointment.user.domain.UsernameGenerator;
import capitec.branch.appointment.sharekernel.EventToJSONMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Base class for all Slot Use Case tests.
 */
abstract class AppointmentTestBase extends AppointmentBookingApplicationTests {

    @Autowired
    protected AppointmentService appointmentService;
    public final static String ADMIN_USERNAME = "admin";
    @Autowired private DeleteBranchUseCase branchUseCase;   //
    @Autowired private AddBranchUseCase addBranchUseCase;
    @Autowired protected KeycloakService keycloakService;
    @Autowired protected FetchRoleByNameService fetchRoleByNameService;
    @Autowired protected UserRoleService userRoleService;
    @Autowired protected SlotService slotService;
    @Autowired protected StaffService staffService;
    @Autowired
    @Qualifier("branchLocationCacheManager")
    protected CacheManager cacheManagerBranchLocationService;
    @Autowired
    @Qualifier("branchCacheManager")
    protected CacheManager cacheManagerBranches;
    @Autowired
    protected CircuitBreakerRegistry circuitBreakerRegistry;
    @Autowired
    protected GetDateOfNextDaysQuery getDateOfNextDaysQuery;
    @Autowired
    private SlotGeneratorScheduler slotGeneratorScheduler;
    @Autowired
    private AddBranchAppointmentInfoUseCase addBranchAppointmentInfoUseCase;

    private WireMock capitecApiWireMock;

    protected Predicate<String> excludeAdmin = username -> !ADMIN_USERNAME.equals(username);
    protected List<String> staff = new ArrayList<>();
    protected List<String> guestClients = new ArrayList<>();
    protected  static  String ADMIN_GROUP = "ADMIN";
    protected static  String GUEST_CLIENT_GROUP = "USER_GUEST";
    protected List<Slot> slots;
    protected  Branch branch;
    //SLOT

    protected ObjectMapper objectMapper = EventToJSONMapper.getMapper();
    @Autowired
    private GetNext7DaySlotsQuery getNext7DaySlotsQuery;


    @BeforeEach
    public void setupBase() {
        setUpBranch();
        setUpStaff();
        setUpCustomers();
        setUpSlots();

        slots = getNext7DaySlotsQuery.execute(branch.getBranchId(),LocalDate.now().plusDays(1))
                .values().stream().flatMap(List::stream).sorted(Comparator.comparing(Slot::getDay))
                .collect(Collectors.toList());
    }

    @AfterEach
    public void tearDownBase() {

        //clear up appointments

        Collection<Appointment> appointments = appointmentService.branchAppointments(branch.getBranchId(),0,Integer.MAX_VALUE);
        for (Appointment appointment : appointments) {

                appointmentService.deleteAppointment(appointment.getId());
            }
        // delete slots
            slotService.getSlots(branch.getBranchId(), LocalDate.now())
                    .forEach(slot -> {
                        slotService.cleanUpSlot(slot.getId());
                    });

        // delete staff
        for (var username : staff) {
            staffService.deleteStaff(username);
        }
        // 2. Delete all test branches
        deleteBranches();

        // 3. Delete all test staff users
        UsersResource usersResource = keycloakService.getRealm().users();
        List<String> staffIds = usersResource
                .list()
                .stream()
                .filter(u -> excludeAdmin.test(u.getUsername()))
                .map(UserRepresentation::getId)
                .collect(Collectors.toList());

        for (var id : staffIds) {
            usersResource.delete(id);
        }
    }

    // --- Utility Methods ---

    private void setUpSlots(){
        slotGeneratorScheduler.execute();
    }

    private void setUpBranch() {
        var branchesString ="SAS293200";

        capitecApiWireMock = new WireMock(
                wiremockContainer.getHost(),
                wiremockContainer.getFirstMappedPort()
        );

        // Reset any previous stubs
        capitecApiWireMock.resetMappings();
        stubCapitecApiSuccess(capitecApiWireMock, capitecApiBranchResponse());
        // Reset circuit breaker state before each test
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("branchLocatorCircuitBreaker");
        cb.reset();

        // Clear caches
        clearCaches();

        BranchDTO branchDTO = new BranchDTO(branchesString);
         branch = addBranchUseCase.execute(branchDTO);

        LocalDate now = LocalDate.now();
        Set<Day> execute = getDateOfNextDaysQuery.execute(now.getDayOfWeek(), now.plusDays(6).getDayOfWeek());

        for(var day : execute) {

            if(day.isHoliday())continue;

            else if(day.isWeekday()){

                BranchAppointmentInfoDTO dto = new BranchAppointmentInfoDTO(
                        6,
                        Duration.ofMinutes(30),
                        0.6,
                        DayType.valueOf(day.getDate().getDayOfWeek().name()),
                        2
                );
                addBranchAppointmentInfoUseCase.execute(branch.getBranchId(), dto);
            }
            else if(day.getDate().getDayOfWeek().equals(DayOfWeek.SATURDAY)){
                BranchAppointmentInfoDTO dto = new BranchAppointmentInfoDTO(
                        4,
                        Duration.ofMinutes(30),
                        0.3,
                        DayType.valueOf(day.getDate().getDayOfWeek().name()),
                        1
                );
                addBranchAppointmentInfoUseCase.execute(branch.getBranchId(), dto);
            }

        }


    }

    private void clearCaches() {
        var cache = new Cache[]{
                cacheManagerBranchLocationService.getCache(CapitecBranchLocationFetcher.BRANCH_LOCATIONS_BY_COORDINATES_CACHE),
                cacheManagerBranchLocationService.getCache(CapitecBranchLocationFetcher.BRANCH_LOCATIONS_BY_AREA_CACHE),
                cacheManagerBranches.getCache(BranchDaoImpl.CACHE_NAME)
        };
        for (Cache cache1 : cache) {
            if (cache1 != null) {
                cache1.clear();
            }
        }
    }

    private void deleteBranches() {
        capitecApiWireMock.resetMappings();
        branchUseCase.execute(branch.getBranchId());
    }
    private void setUpCustomers() {
        UsersResource usersResource = keycloakService.getRealm().users();
        String[] users = new String[]{
                "sara.lee@capitec.co.za;Sara;Lee;k9aL!pG5rT2q",
                "thabo.mokoena@vodacom.co.za;Thabo;Mokoena;4gHj#zX7yW0b",
                "nandi.sibanda@sun.ac.za;Nandi;Sibanda;m7cE$aD3vI8l",
                "gregoryhill@gmail.com;Gregory;Hill;p1zQ%oN9kF4s",
                "phumzile@uwc.ac.za;Phumzile;Nkosi;t3jB&xR6uY5a",
                "lebo_m@outlook.com;Lebo;Masilela;q6iD*hU2wV7e",
                "liam.jones@fnb.co.za;Liam;Jones;e8yK+cJ4sF1g",
                "zola.dube@dstv.com;Zola;Dube;r2fA/tP0hJ3c",
                "aisha.omar@wits.ac.za;Aisha;Omar;w5nB?mL1xT9d",
                "carlv@absac.co.za;Carl;VanWyk;v4sZ@bQ8cP6f"
        };

        for (String user : users) {
            String[] userProp = user.split(";");
            UserRepresentation userRepresentation = new UserRepresentation();
            String username = new UsernameGenerator().getId();
            userRepresentation.setUsername(username);
            guestClients.add(username);
            userRepresentation.setEmail(userProp[0]);
            userRepresentation.setFirstName(userProp[1]);
            userRepresentation.setLastName(userProp[2]);
            userRepresentation.setEnabled(true);
            userRepresentation.setEmailVerified(true);
            CredentialRepresentation credentialRepresentation = new CredentialRepresentation();
            credentialRepresentation.setType("password");
            credentialRepresentation.setValue(userProp[3]);
            userRepresentation.setCredentials(List.of(credentialRepresentation));
            usersResource.create(userRepresentation);

            String adminId = fetchRoleByNameService.getGroupId(GUEST_CLIENT_GROUP, true).orElseThrow();
            userRoleService.addUserToGroup(username, adminId);
        }
    }
    private void setUpStaff() {
        UsersResource usersResource = keycloakService.getRealm().users();
        String[] users = new String[]{
                "manju@gmail.com;Manju;Miranda;@KrVgfjl62",
                "ram@gmail.com;Evgeniy;Ram;@KrVgfjl6t",
                "yu@gmail.com;Yu;Ning;@KrVgfjl65",
                "dorismia@myuct.ac.za;Doris;Mia;@KrVgfjl65",
                "davidchong@cput.ac.za;David;Chong;1wcB2OsQFV6_"
        };

        for (String user : users) {
            String[] userProp = user.split(";");
            UserRepresentation userRepresentation = new UserRepresentation();
            String username = new UsernameGenerator().getId();
            userRepresentation.setUsername(username);
            userRepresentation.setEmail(userProp[0]);
            userRepresentation.setFirstName(userProp[1]);
            userRepresentation.setLastName(userProp[2]);
            userRepresentation.setEnabled(true);
            userRepresentation.setEmailVerified(true);
            CredentialRepresentation credentialRepresentation = new CredentialRepresentation();
            credentialRepresentation.setType("password");
            credentialRepresentation.setValue(userProp[3]);
            userRepresentation.setCredentials(List.of(credentialRepresentation));
            usersResource.create(userRepresentation);

            String adminId = fetchRoleByNameService.getGroupId(ADMIN_GROUP, true).orElseThrow();
            userRoleService.addUserToGroup(username, adminId);
            staffService.addStaff(new Staff(username, StaffStatus.WORKING,branch.getBranchId()));
            staff.add(username);

        }


    }

    protected Optional<EventValue<String,AppointmentMetadata>> getLatestRecordForKey(
            Consumer<String, String> consumer,
            String key,
            Duration timeout) {

        ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(consumer, timeout);
        return StreamSupport.stream(records.spliterator(), false)
                .filter(record -> record.key().equals(key))
                .map(record ->{
                    String value = record.value();

                    try {

                        EventValue<String,AppointmentMetadata> eventValue = record.topic().endsWith(".retry") ?
                                objectMapper.readValue(value, new TypeReference<EventValue.EventError<String, AppointmentMetadata>>() {}):
                                objectMapper.readValue(value, new TypeReference<EventValue.OriginEventValue<String, AppointmentMetadata>>() {});
                        return eventValue;
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .max(Comparator.comparing(
                        eventValue->
                        eventValue.value().createdAt()
                ));
    }

}