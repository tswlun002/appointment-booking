package capitec.branch.appointment.user.infrastructure.keycloak;

import capitec.branch.appointment.AppointmentBookingApplicationTests;
import capitec.branch.appointment.exeption.EntityAlreadyExistException;
import capitec.branch.appointment.keycloak.domain.KeycloakService;
import capitec.branch.appointment.user.app.port.UserPersistencePort;
import capitec.branch.appointment.user.app.port.UserQueryPort;
import capitec.branch.appointment.user.domain.User;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.AbstractUserRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import static capitec.branch.appointment.utils.KeycloakUtils.keyCloakRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;


@Slf4j
public class UserServiceImplTest  extends AppointmentBookingApplicationTests {


    public final static String username = "admin";
    @Value("${default_roles.admin}")
    private List<String> defaultAdminRoles;
    @Value("${default_roles.users}")
    private List<String> defaultUsersRoles;
    @Autowired
    private UserPersistencePort userPersistencePort;
    @Autowired
    private UserQueryPort userQueryPort;
    @Autowired
    private  KeycloakService keycloakService;


    Predicate<UserRepresentation> predicate = u -> !u.getUsername().equals(username);

    @BeforeEach
    void setUp() {


        UsersResource usersResource = keycloakService.getUsersResources();
        List<UserRepresentation> list = keyCloakRequest(usersResource::list, "get users", UserRepresentation.class);

        list.stream().filter(predicate).map(AbstractUserRepresentation::getId).forEach(id -> {
            try (var a = usersResource.delete(id)) {
            }
        });



        RolesResource rolesResource = keycloakService.getClientRolesResource();
        List<RoleRepresentation> list1 = rolesResource.list().stream().filter(role ->
                !(  defaultUsersRoles.contains(role.getName()) ||
                        defaultAdminRoles.contains(role.getName())
                )
        ).toList();
        list1.forEach(role -> {
            RoleResource roleResource = rolesResource.get(role.getName());
            roleResource.remove();
        });
    }

    @AfterEach
    void tearDown() {

        UsersResource usersResource = keycloakService.getUsersResources();

        List<UserRepresentation> list = usersResource.list();

        list.stream().filter(predicate).map(AbstractUserRepresentation::getId).forEach(id -> {
            try (var a = usersResource.delete(id)) {

            }
        });

    }

    @ParameterizedTest
    @CsvSource(value = {"nklsip001@gmail.com;White;Joel;@NkLlun00033"}, delimiter = ';')
    void registerUserValidUser(String email, String firstName, String lastName, String password) {
        var user = new User(email, firstName, lastName, password);
        var userEntity = userPersistencePort.registerUser(user);
        assertThat(userEntity).isNotNull()
                .isExactlyInstanceOf(User.class)
                .hasFieldOrPropertyWithValue("email", email)
                .hasFieldOrPropertyWithValue("firstname", firstName)
                .hasFieldOrPropertyWithValue("lastname", lastName)
                .hasFieldOrPropertyWithValue("verified", false)
                .hasFieldOrPropertyWithValue("enabled", true)
                .hasFieldOrPropertyWithValue("username", user.getUsername());


        var userRep = keycloakService.getUsersResources().searchByUsername(user.getUsername(),true).stream().findFirst();

        assertThat(userRep.isPresent()).isTrue();
        boolean action = userRep.get().getRequiredActions().getFirst().equals("VERIFY_EMAIL");
        assertThat(action).isTrue();

        // Two users, client user(default) when create keycloak and current user we registered above
        assertThat(keycloakService.getUsersResources().count()).isEqualTo(2);
    }

    @ParameterizedTest
    @CsvSource(value = {"nklsip001@gmail.com;White;Joel;@NkLlun00033"}, delimiter = ';')
    void registerDuplicatedUser(String email, String firstName, String lastName, String password) {

        var user = new User(email, firstName, lastName, password);
        var user2 = new User(email, "ElisabethCeng",
                "DonnaLeon", "6Bb30201-6668-4e54-9d2b-85a3374a82f2");
        user = userPersistencePort.registerUser(user);
        assertThatException().isThrownBy(() -> userPersistencePort.registerUser(user2))
                .isExactlyInstanceOf(EntityAlreadyExistException.class)
                .withMessage("User already exists");
        assertThat(user).isNotNull()
                .isExactlyInstanceOf(User.class)
                .hasFieldOrPropertyWithValue("email", email)
                .hasFieldOrPropertyWithValue("firstname", firstName)
                .hasFieldOrPropertyWithValue("lastname", lastName)
                .hasFieldOrPropertyWithValue("verified", false)
                .hasFieldOrPropertyWithValue("enabled", true)
                .hasFieldOrPropertyWithValue("username", user.getUsername());
        var users = keycloakService.getUsersResources();
        assertThat(users.count()).isEqualTo(2);
    }

    @ParameterizedTest
    @CsvSource(value = {"nklsip001@gmail.com;White;Joel;@NkLlun00033"}, delimiter = ';')
    void verifySavedUser(String email, String firstName, String lastName, String password) {

        var user = new User(email, firstName, lastName, password);
        user = userPersistencePort.registerUser(user);
        assertThat(user).isNotNull()
                .hasFieldOrPropertyWithValue("verified", false);
        boolean verifyUser = userPersistencePort.verifyUser(user.getUsername());
        assertThat(verifyUser).isTrue();
        user = userQueryPort.getUserByUsername(user.getUsername()).orElseThrow();

        assertThat(user).isNotNull()
                .hasFieldOrPropertyWithValue("verified", true);
        var  userRep = keycloakService.getUsersResources().searchByUsername(user.getUsername(), true).stream().findFirst();
        assertThat(userRep.isPresent()).isTrue();
        var actions = userRep.get().getRequiredActions();
        assertThat(actions.isEmpty()).isTrue();

    }


    @ParameterizedTest
    @CsvSource(value = {"nklsip001@gmail.com;White;Joel;@NkLlun00033"}, delimiter = ';')
    void deleteSavedUser(String email, String firstName, String lastName, String password) {
        var user = new User(email, firstName, lastName, password);
        var userEntity = userPersistencePort.registerUser(user);
        var expected = userQueryPort.getUserByUsername(user.getUsername()).orElseThrow();
        assertThat(userEntity.getUsername()).isEqualTo(expected.getUsername());
        var users = keycloakService.getUsersResources();
        assertThat(users.count()).isEqualTo(2);
        userPersistencePort.verifyUser(user.getUsername());
        var isDeleted = userPersistencePort.deleteUser(user.getUsername());
        assertThat(isDeleted).isTrue();
        var first = users.searchByEmail(email, true);
        assertThat(first).isEmpty();
    }

    @ParameterizedTest
    @CsvSource(value = {"38310942437"}, delimiter = ';')
    void deleteNoneExistingUser(String username) {
        assertThatException().isThrownBy(() -> userPersistencePort.deleteUser(username))
                .isExactlyInstanceOf(ResponseStatusException.class)
                .withMessageContaining("User not found");

    }

    @ParameterizedTest
    @CsvSource(value = {"nadiamanuel1@gmail.com;Nadia;manuel;@NkLlun00033"}, delimiter = ';')
    public void getUserByUsernameExists(String email, String firstName, String lastName, String password) {
        var user = new User(email, firstName, lastName, password);
        user= userPersistencePort.registerUser(user);
        var user1 = userQueryPort.getUserByUsername(user.getUsername()).orElseThrow();
        assertThat(user1).isNotNull()
                .hasFieldOrPropertyWithValue("verified", user.isVerified())
                .hasFieldOrPropertyWithValue("enabled", user.isEnabled())
                .hasFieldOrPropertyWithValue("username", user.getUsername())
                .hasFieldOrPropertyWithValue("email", user.getEmail())
                .hasFieldOrPropertyWithValue("firstname", firstName)
                .hasFieldOrPropertyWithValue("lastname", lastName)
                .hasFieldOrPropertyWithValue("password", null);

    }

    @ParameterizedTest
    @CsvSource(value = {"3344778899"}, delimiter = ';')
    public void getUserByUsernameDoneNotExists(String username) {
        var user = userQueryPort.getUserByUsername(username);
        assertThat(user.isEmpty()).isTrue();


    }

    @ParameterizedTest
    @CsvSource(value = {"nadiamanuel1@gmail.com;Nadia;manuel;@NkLlun00033"}, delimiter = ';')
    public void getUserByEmailExists(String email, String firstName, String lastName, String password) {
        var user = new User(email, firstName, lastName, password);
        userPersistencePort.registerUser(user);
        user = userQueryPort.getUserByEmail(user.getEmail()).orElseThrow();
        assertThat(user).isNotNull()
                .hasFieldOrPropertyWithValue("verified", user.isVerified())
                .hasFieldOrPropertyWithValue("enabled", user.isEnabled())
                .hasFieldOrPropertyWithValue("username", user.getUsername())
                .hasFieldOrPropertyWithValue("email", user.getEmail())
                .hasFieldOrPropertyWithValue("firstname", firstName)
                .hasFieldOrPropertyWithValue("lastname", lastName);

    }

    @ParameterizedTest
    @CsvSource(value = {"nadiamanuel1@gmail.com"}, delimiter = ';')
    public void getUserByEmailDoneNotExists(String emil) {
        Optional<User> user = userQueryPort.getUserByEmail(emil);
        assertThat(user.isEmpty()).isTrue();
    }

}