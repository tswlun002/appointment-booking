package capitec.branch.appointment.appointment.app;

import capitec.branch.appointment.AppointmentBookingApplicationTests;
import capitec.branch.appointment.appointment.domain.Appointment;
import capitec.branch.appointment.appointment.domain.AppointmentService;
import capitec.branch.appointment.branch.app.AddBranchUseCase;
import capitec.branch.appointment.branch.app.BranchDTO;
import capitec.branch.appointment.branch.app.DeleteBranchUseCase;
import capitec.branch.appointment.branch.domain.Branch;
import capitec.branch.appointment.branch.domain.address.Address;
import capitec.branch.appointment.keycloak.domain.KeycloakService;
import capitec.branch.appointment.role.domain.FetchRoleByNameService;
import capitec.branch.appointment.slots.app.GenerateSlotsUseCase;
import capitec.branch.appointment.slots.domain.Slot;
import capitec.branch.appointment.slots.domain.SlotService;
import capitec.branch.appointment.staff.domain.Staff;
import capitec.branch.appointment.staff.domain.StaffService;
import capitec.branch.appointment.staff.domain.StaffStatus;
import capitec.branch.appointment.user.domain.UserRoleService;
import capitec.branch.appointment.user.domain.UsernameGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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

    protected Predicate<String> excludeAdmin = username -> !ADMIN_USERNAME.equals(username);
    protected List<String> staff = new ArrayList<>();
    protected List<String> guestClients = new ArrayList<>();
    protected List<Branch> branches = new ArrayList<>();
    protected  static  String ADMIN_GROUP = "ADMIN";
    protected static  String GUEST_CLIENT_GROUP = "USER_GUEST";
    protected List<Slot> slots;
    protected  String branchId;

    //SLOT
    protected final LocalDate TODAY = LocalDate.now().plusDays(1);
    protected final LocalDate TOMORROW = LocalDate.now().plusDays(2);
    protected final LocalDate DAY_AFTER = LocalDate.now().plusDays(3);
    protected final int  MAX_BOOKING_CAPACITY = 2;

    @BeforeEach
    public void setupBase() {

        setUpBranch();
        branchId = branches.getFirst().getBranchId();
        setUpStaff();
        setUpCustomers();
        setUpSlots();
        slots = slotService.getNext7DaySlots(branchId,TODAY);
      //  UsersResource usersResource = keycloakService.getRealm().users();



    }

    @AfterEach
    public void tearDownBase() {

        //clear up appointments

        Collection<Appointment> appointments = appointmentService.branchAppointments(branchId,0,Integer.MAX_VALUE);
        for (Appointment appointment : appointments) {

                appointmentService.deleteAppointment(appointment.getId());
            }
        // delete slots
            slotService.getNext7DaySlots(branchId, LocalDate.now())
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

    protected void setUpSlots(){


        // Example Slots for arrangement
         final Slot slot1 = new Slot(TODAY, LocalTime.of(9, 0), LocalTime.of(9, 30), MAX_BOOKING_CAPACITY, branchId);
         final Slot slot2 = new Slot(TODAY, LocalTime.of(9, 30), LocalTime.of(10, 0), MAX_BOOKING_CAPACITY, branchId);
         final Slot slot3 = new Slot(TOMORROW, LocalTime.of(9, 0), LocalTime.of(9, 30), MAX_BOOKING_CAPACITY, branchId);
         final Slot slot4 = new Slot(TOMORROW, LocalTime.of(9, 30), LocalTime.of(10, 0), MAX_BOOKING_CAPACITY, branchId);
         final Slot slot5 = new Slot(DAY_AFTER, LocalTime.of(8, 0), LocalTime.of(8, 30), MAX_BOOKING_CAPACITY, branchId);

        slotService.save(List.of(slot1, slot2, slot3, slot4, slot5));


    }

    protected void setUpBranch() {
        var branchesString = new String[]{
                "BR001;09:00;17:00;123;Main Street;Rosebank;Johannesburg;Gauteng;2196;South Africa",
             //   "BR002;08:30;16:30;456;Church Street;Hatfield;Pretoria;Gauteng;2828;South Africa",
        };

        for (String branch : branchesString) {
            String[] branchInfo = branch.split(";");
            Address address = new Address(branchInfo[3], branchInfo[4], branchInfo[5], branchInfo[6], branchInfo[7], Integer.parseInt(branchInfo[8]), branchInfo[9]);
            BranchDTO branchDTO = new BranchDTO(branchInfo[0], LocalTime.parse(branchInfo[1]), LocalTime.parse(branchInfo[2]), address);
            branches.add(addBranchUseCase.execute(branchDTO));
        }
    }

    protected void deleteBranches() {
        for (Branch branch : branches) {
            branchUseCase.execute(branch.getBranchId());
        }
    }
    protected void setUpCustomers() {
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
    protected void setUpStaff() {
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
            staffService.addStaff(new Staff(username, StaffStatus.WORKING,branchId));
            staff.add(username);

        }


    }
}