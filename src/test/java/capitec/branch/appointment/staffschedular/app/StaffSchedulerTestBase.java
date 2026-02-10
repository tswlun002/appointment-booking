package capitec.branch.appointment.staffschedular.app;

import capitec.branch.appointment.AppointmentBookingApplicationTests;
import capitec.branch.appointment.branch.app.AddBranchUseCase;
import capitec.branch.appointment.branch.app.BranchDTO;
import capitec.branch.appointment.branch.app.DeleteBranchUseCase;
import capitec.branch.appointment.branch.domain.Branch;
import capitec.branch.appointment.branch.infrastructure.dao.BranchDaoImpl;
import capitec.branch.appointment.keycloak.domain.KeycloakService;
import capitec.branch.appointment.location.infrastructure.api.CapitecBranchLocationFetcher;
import capitec.branch.appointment.role.domain.FetchRoleByNameService;
import capitec.branch.appointment.user.app.port.UserRoleService;
import capitec.branch.appointment.user.domain.UsernameGenerator;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Base class for all Staff Scheduler tests, handling Keycloak staff and Branch data management.
 */
abstract class StaffSchedulerTestBase extends AppointmentBookingApplicationTests {

    public final static String ADMIN_USERNAME = "admin";
    
    @Autowired private DeleteBranchUseCase branchUseCase;   //
    @Autowired private AddBranchUseCase addBranchUseCase;
    @Autowired protected KeycloakService keycloakService;
    @Autowired protected FetchRoleByNameService fetchRoleByNameService;
    @Autowired protected UserRoleService userRoleService;
    @Autowired
    @Qualifier("branchLocationCacheManager")
    protected CacheManager cacheManagerBranchLocationService;
    @Autowired
    @Qualifier("branchCacheManager")
    protected CacheManager cacheManagerBranches;
    @Autowired
    protected CircuitBreakerRegistry circuitBreakerRegistry;

    protected Predicate<String> excludeAdmin = username -> !ADMIN_USERNAME.equals(username);
    protected List<String> staff;
    protected List<Branch> branches = new ArrayList<>();

    private WireMock capitecApiWireMock;

    @BeforeEach
    public void setupBase() {
        setUpBranch();
        setUpStaff();
        
        UsersResource usersResource = keycloakService.getRealm().users();
        staff = usersResource
                .list()
                .stream()
                .map(UserRepresentation::getUsername)
                .filter(excludeAdmin).toList();
    }

    @AfterEach
    public void tearDownBase() {
        //1. Delete all test staff users
        UsersResource usersResource = keycloakService.getRealm().users();
        List<String> staffIds = usersResource
                .list()
                .stream()
                .filter(u -> excludeAdmin.test(u.getUsername()))
                .map(UserRepresentation::getId)
                .toList();

        for (var id : staffIds) {
            usersResource.delete(id);
        }
        // 2. Delete all test branches
        deleteBranches();

    }

    // --- Utility Methods ---

    protected void setUpBranch() {
        var branchesString = new String[]{
                "SAS293200",
                "SAS29300",
        };

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

        for (String branch : branchesString) {
            String[] branchInfo = branch.split(";");
            BranchDTO branchDTO = new BranchDTO(branchInfo[0]);
            branches.add(addBranchUseCase.execute(branchDTO));
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

    protected void deleteBranches() {
        capitecApiWireMock.resetMappings();
        for (Branch branch : branches) {
            branchUseCase.execute(branch.getBranchId());
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

            String adminId = fetchRoleByNameService.getGroupId("ADMIN", true).orElseThrow();
            userRoleService.addUserToGroup(username, adminId);
        }
    }
}