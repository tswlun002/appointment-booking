package capitec.branch.appointment.staff.app;


import capitec.branch.appointment.AppointmentBookingApplicationTests;
import capitec.branch.appointment.keycloak.domain.KeycloakService;
import capitec.branch.appointment.role.domain.FetchRoleByNameService;
import capitec.branch.appointment.staff.domain.Staff;
import capitec.branch.appointment.staff.domain.StaffStatus;
import capitec.branch.appointment.user.domain.UserRoleService;
import capitec.branch.appointment.user.domain.UsernameGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static capitec.branch.appointment.staff.domain.StaffStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StaffUseCaseTest extends AppointmentBookingApplicationTests {



    public final static String username = "admin";
    @Autowired
    private KeycloakService keycloakService;
    @Autowired
    private  FetchRoleByNameService fetchRoleByNameService;
    @Autowired
    private  UserRoleService userRoleService;
    @Autowired
    private StaffUseCase staffUseCase;

    private List<String> staff;

    Predicate<String> excludeAdmin = username->!StaffUseCaseTest.username.equals(username);

    @BeforeEach
    public void setup() {

        setUpStaff();
        UsersResource usersResource = keycloakService.getRealm().users();
       staff= usersResource.list().stream().map(UserRepresentation::getUsername)
               .filter(excludeAdmin).toList();

    }
    @AfterEach
    public void tearDown() {

        UsersResource usersResource = keycloakService.getRealm().users();
        List<String> staffIds = usersResource.list().stream()
                .filter(u->excludeAdmin.test(u.getUsername()))
                .map(UserRepresentation::getId).toList();

        for(var id : staffIds){
            usersResource.delete(id);
        }
    }


    @ParameterizedTest
    @ValueSource(strings = "59f3c768-9712-423c-a940-9a873a4934fb")
    void addStaff(String branchId) {

        for(var staff: staff){
            StaffDTO staffDTO = new StaffDTO(staff, branchId);
            boolean isAdded = staffUseCase.addStaff(staffDTO);
            assertThat(isAdded).isTrue();
        }

        int staffCount = staffUseCase.getStaffByBranchIdAndStatus(branchId, TRAINING).size();

        assertThat(staffCount).isEqualTo(staff.size());


    }
    @ParameterizedTest
    @ValueSource(strings = "59f3c768-9712-423c-a940-9a873a4934fb")
    void updateStaffStatusExistingStaff(String branchId) {

        for(var staff: staff){
            StaffDTO staffDTO = new StaffDTO(staff, branchId);
            boolean isAdded = staffUseCase.addStaff(staffDTO);
            assertThat(isAdded).isTrue();
        }

        int halfStaffSize = staff.size() / 2;

        List<String> firstHalf = staff.subList(0, halfStaffSize);
        List<String> secondHalf = staff.subList(halfStaffSize, staff.size());
       for(var staff: firstHalf){
           var updated = staffUseCase.updateStaff(staff, WORKING) ;
           assertThat(updated).isNotNull();
           assertThat(updated.status()).isEqualTo(WORKING);
       }
       for(var staff: secondHalf){
           var updated = staffUseCase.updateStaff(staff, LEAVE) ;
           assertThat(updated).isNotNull();
           assertThat(updated.status()).isEqualTo(LEAVE);
       }

        Set<Staff> staffByBranchIdAndStatus = staffUseCase.getStaffByBranchIdAndStatus(branchId, StaffStatus.WORKING);
       assertThat(staffByBranchIdAndStatus.size()).isEqualTo(halfStaffSize);
        boolean containsAll = staffByBranchIdAndStatus.stream().allMatch(s->firstHalf.contains(s.username()));
        assertThat(containsAll).isTrue();

        Set<Staff> staffByBranchIdAndStatus1 = staffUseCase.getStaffByBranchIdAndStatus(branchId, LEAVE);
        assertThat(staffByBranchIdAndStatus1.size()).isEqualTo((staff.size()-halfStaffSize));
        boolean containsAll1 = staffByBranchIdAndStatus1.stream().allMatch(s->secondHalf.contains(s.username()));
        assertThat(containsAll1).isTrue();


    }
    @ParameterizedTest
    @ValueSource(strings = "59f3c768-9712-423c-a940-9a873a4934fb")
    void updateStaffStatusDoesNotExisting(String branchId) {
        int halfStaffSize = staff.size() / 2;

        List<String> secondHalf = staff.subList(halfStaffSize, staff.size());

        for(var staff: secondHalf){

            assertThatThrownBy(()->staffUseCase.updateStaff(staff, LEAVE))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("404 NOT_FOUND \"Staff not found\"");

        }

        Set<Staff> staffByBranchIdAndStatus1 = staffUseCase.getStaffByBranchIdAndStatus(branchId, LEAVE);
        assertThat(staffByBranchIdAndStatus1.size()).isEqualTo(0);

    }

    public  void setUpStaff() {

        UsersResource usersResource = keycloakService.getRealm().users();
        String[] users  = new String[] {
                "manju@gmail.com;Manju;Miranda;@KrVgfjl62",
                "ram@gmail.com;Evgeniy;Ram;@KrVgfjl6t",
                "yu@gmail.com;Yu;Ning;@KrVgfjl65",
                "dorismia@myuct.ac.za;Doris;Mia;@KrVgfjl65",
                "davidchong@cput.ac.za;David;Chong;1wcB2OsQFV6_"
        };

        for(String user : users) {

            String[] userProp = user.split(";");
            UserRepresentation userRepresentation = new UserRepresentation();
            String username= new UsernameGenerator().getId();
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

            String adminId = fetchRoleByNameService.getGroupId("ADMIN", true).orElseThrow();
            userRoleService.addUserToGroup(username, adminId);

        }


    }

}