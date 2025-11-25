package capitec.branch.appointment.user.infrastructure.keycloak;

import jakarta.ws.rs.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import capitec.branch.appointment.AppointmentBookingApplicationTests;
import capitec.branch.appointment.keycloak.domain.KeycloakService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;
import capitec.branch.appointment.exeption.EntityAlreadyExistException;
import capitec.branch.appointment.role.domain.Role;
import capitec.branch.appointment.role.domain.RoleService;
import capitec.branch.appointment.user.domain.USER_TYPES;
import capitec.branch.appointment.user.domain.User;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.AbstractUserRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import static capitec.branch.appointment.utils.KeycloakUtils.keyCloakRequest;

@Slf4j
public class UserServiceImplTest  extends AppointmentBookingApplicationTests {


    public final static String username = "admin";
    @Value("${default_roles.admin}")
    private List<String> defaultAdminRoles;
    @Value("${default_roles.users}")
    private List<String> defaultUsersRoles;
    @Autowired
    UserServiceImpl userService;
    @Autowired
    RoleService roleService;
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
        var userEntity = userService.registerUser(user);
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
        user = userService.registerUser(user);
        assertThatException().isThrownBy(() -> userService.registerUser(user2))
                .isExactlyInstanceOf(EntityAlreadyExistException.class)
                .withMessage("User already exist.");
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
        user = userService.registerUser(user);
        assertThat(user).isNotNull()
                .hasFieldOrPropertyWithValue("verified", false);
        boolean verifyUser = userService.verifyUser(user.getUsername());
        assertThat(verifyUser).isTrue();
        user = userService.getUserByUsername(user.getUsername()).orElseThrow();

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
        var userEntity = userService.registerUser(user);
        var expected = userService.getUserByUsername(user.getUsername()).orElseThrow();
        assertThat(userEntity.getUsername()).isEqualTo(expected.getUsername());
        var users = keycloakService.getUsersResources();
        assertThat(users.count()).isEqualTo(2);
        userService.verifyUser(user.getUsername());
        var isDeleted = userService.deleteUser(user.getUsername());
        assertThat(isDeleted).isTrue();
        var first = users.searchByEmail(email, true);
        assertThat(first).isEmpty();
    }

    @ParameterizedTest
    @CsvSource(value = {"38310942437"}, delimiter = ';')
    void deleteNoneExistingUser(String username) {
        assertThatException().isThrownBy(() -> userService.deleteUser(username))
                .isExactlyInstanceOf(NotFoundException.class)
                .withMessage("User not found");

    }

    @ParameterizedTest
    @CsvSource(value = {"nadiamanuel1@gmail.com;Nadia;manuel;@NkLlun00033"}, delimiter = ';')
    public void getUserByUsernameExists(String email, String firstName, String lastName, String password) {
        var user = new User(email, firstName, lastName, password);
        user= userService.registerUser(user);
        var user1 = userService.getUserByUsername(user.getUsername()).orElseThrow();
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
        var user = userService.getUserByUsername(username);
        assertThat(user.isEmpty()).isTrue();


    }

    @ParameterizedTest
    @CsvSource(value = {"nadiamanuel1@gmail.com;Nadia;manuel;@NkLlun00033"}, delimiter = ';')
    public void getUserByEmailExists(String email, String firstName, String lastName, String password) {
        var user = new User(email, firstName, lastName, password);
        userService.registerUser(user);
        user = userService.getUserByEmail(user.getEmail()).orElseThrow();
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
        Optional<User> user = userService.getUserByEmail(emil);
        assertThat(user.isEmpty()).isTrue();
    }

    @ParameterizedTest
    @CsvSource(value = {"nadiamanuel1@gmail.com;Nadia;manuel;@NkLlun00033"}, delimiter = ';')
    public void assignValidRoleTValidUser(String email, String firstName, String lastName, String password) {

        var user = new User(email, firstName, lastName, password);

        userService.registerUser(user);

        Set<String> collect = defaultUsersRoles.stream().map(r ->
                roleService.getClientRole(r).orElseThrow().getId()
        ).collect(Collectors.toSet());

        collect.forEach(r-> {

            boolean b = userService.assignRoleToUser(user.getUsername(), r);

            assertThat(b).isTrue();
        });

    }

    @ParameterizedTest
    @CsvSource(value = {"nadiamanuel1@gmail.com;Nadia;manuel;@NkLlun00033;app_users;Allow to create user to creat account;true"}, delimiter = ';')
    public void getAssignedRoleTUser(String email, String firstName, String lastName, String password,
                                     String roleName, String roleDescription, boolean isClientRole) {
        var user = new User(email, firstName, lastName, password);
        userService.registerUser(user);
        Role role = new Role( roleName, roleDescription, isClientRole);
        roleService.createRole(role);

        userService.assignRoleToUser( user.getUsername(),role.getId());
        Collection<String> clientRolesForUser = userService.getUserRoles(user.getUsername());
        Optional<String> first = clientRolesForUser.stream().findFirst();
        assertThat(first.isPresent()).isTrue();

        assertThat(first.get()).isEqualTo(role.id());
    }

    @ParameterizedTest
    @CsvSource(value = {"nadiamanuel1@gmail.com;Nadia;manuel;@NkLlun00033;USERS","barbaraandrade1@gmail.com;Barbara;Andrade;@NkLlun00033;ADMIN"}, delimiter = ';')
    public void addUserToGroup(String email, String firstName, String lastName, String password, USER_TYPES roleType) {

        var user = new User(email, firstName, lastName, password);
        userService.registerUser(user);
        String id = roleService.getGroup(roleType.name(), false).orElseThrow().getId();
        userService.addUserToGroup(user.getUsername(), id);

        UsersResource users = keycloakService.getUsersResources();
        var first = users.searchByUsername( user.getUsername(), true).getFirst();
        UserResource userResource = users.get(first.getId());
        List<GroupRepresentation> groups = userResource.groups();
        boolean b = groups.stream().anyMatch(ug -> ug.getId().equals(id));

        assertThat(b).isTrue();
    }

}