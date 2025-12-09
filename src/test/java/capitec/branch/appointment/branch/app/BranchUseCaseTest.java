package capitec.branch.appointment.branch.app;


import capitec.branch.appointment.AppointmentBookingApplicationTests;
import capitec.branch.appointment.branch.domain.address.Address;
import capitec.branch.appointment.keycloak.domain.KeycloakService;
import capitec.branch.appointment.role.domain.FetchRoleByNameService;
import capitec.branch.appointment.staff.app.StaffDTO;
import capitec.branch.appointment.staff.app.StaffUseCase;
import capitec.branch.appointment.user.domain.UserRoleService;
import capitec.branch.appointment.user.domain.UsernameGenerator;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalTime;
import java.util.List;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

class BranchUseCaseTest  extends AppointmentBookingApplicationTests {

    @Autowired
    private BranchUseCase branchUseCase;
    public final static String username = "admin";
    @Autowired
    private KeycloakService keycloakService;
    @Autowired
    private FetchRoleByNameService fetchRoleByNameService;
    @Autowired
    private UserRoleService userRoleService;
    @Autowired
    private StaffUseCase staffUseCase;



   private Predicate<String> excludeAdmin = username->!BranchUseCaseTest.username.equals(username);

    @BeforeEach
    public void setup() {

        setUpStaff();
        UsersResource usersResource = keycloakService.getRealm().users();
        List<String> staff= usersResource.list().stream().map(UserRepresentation::getUsername)
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
    @CsvSource(delimiter = ';', value = {
            "BR001;09:00;17:00;123;Main Street;Rosebank;Johannesburg;Gauteng;2196;South Africa",
            "BR002;08:30;16:30;456;Church Street;Hatfield;Pretoria;Gauteng;0028;South Africa",
            "BR003;10:00;18:00;789;Long Street;City Centre;Cape Town;Western Cape;8001;South Africa"
    })
    public void addBranch(String branchId, LocalTime openTime, LocalTime closingTime,
                          String streetNumber, String streetName, String suburbs,
                          String city, String province, Integer postalCode, String country) {

//        for(var staff: staff){
//            StaffDTO staffDTO = new StaffDTO(staff, branchId);
//            boolean isAdded = staffUseCase.addStaff(staffDTO);
//        }

        // Arrange
        Address address = new Address(streetNumber, streetName, suburbs, city, province, postalCode, country);
        BranchDTO branchDTO = new BranchDTO(branchId, openTime, closingTime, address);

        // Act
        boolean isAdded = branchUseCase.addBranch(branchDTO);

        // Assert
        assertThat(isAdded).isTrue();
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