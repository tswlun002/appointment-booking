package capitec.branch.appointment.staff.app;

import capitec.branch.appointment.AppointmentBookingApplicationTests;
import capitec.branch.appointment.branch.domain.Branch;
import capitec.branch.appointment.branch.domain.BranchService;
import capitec.branch.appointment.branch.infrastructure.BranchDaoImpl;
import capitec.branch.appointment.keycloak.domain.KeycloakService;
import capitec.branch.appointment.location.infrastructure.api.CapitecBranchLocationFetcher;
import capitec.branch.appointment.role.domain.FetchRoleByNameService;
import capitec.branch.appointment.staff.domain.StaffService;
import capitec.branch.appointment.user.domain.UserRoleService;
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

import java.util.List;
import java.util.function.Predicate;

/**
 * Base class for all Staff Use Case tests, handling common Keycloak setup and teardown.
 */
abstract class StaffUseCaseTestBase extends AppointmentBookingApplicationTests {

    public final static String ADMIN_USERNAME = "admin";
    
    @Autowired
    protected KeycloakService keycloakService;
    @Autowired
    protected FetchRoleByNameService fetchRoleByNameService;
    @Autowired
    protected UserRoleService userRoleService;
    @Autowired
    private  BranchService branchService;
    @Autowired private StaffService staffService;
    @Autowired
    @Qualifier("branchLocationCacheManager")
    protected CacheManager cacheManagerBranchLocationService;
    @Autowired
    @Qualifier("branchCacheManager")
    protected CacheManager cacheManagerBranches;
    @Autowired
    protected CircuitBreakerRegistry circuitBreakerRegistry;
    protected List<String> staff;
    protected   Branch branch;
    private  WireMock capitecApiWireMock;

    protected Predicate<String> excludeAdmin = username -> !ADMIN_USERNAME.equals(username);

    @BeforeEach
    public void setupBase() {
        setUpTestUsers();
        setUpBranch();
        UsersResource usersResource = keycloakService.getRealm().users();
        staff = usersResource.list().stream()
                .map(UserRepresentation::getUsername)
                .filter(excludeAdmin)
                .toList();
    }

    @AfterEach
    public void tearDownBase() {
        for (String username : staff) {
            staffService.deleteStaff(username);
        }
        deleteBranches();
        UsersResource usersResource = keycloakService.getRealm().users();
        // Delete only the test staff users created by setUpTestStaffUsers
        List<String> users = usersResource.list().stream()
                .filter(u -> excludeAdmin.test(u.getUsername()))
                .map(UserRepresentation::getId)
                .toList();

        for (var id : users) {
            usersResource.delete(id);
        }
        capitecApiWireMock.resetMappings();
    }
    protected void setUpBranch() {
        var branchString = "SAS29300";
                //   "BR002;08:30;16:30;456;Church Street;Hatfield;Pretoria;Gauteng;2828;South Africa",


        branch = new Branch(branchString,"Capitec Head Office");
        branch =branchService.add(branch);

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

       branchService.delete(branch.getBranchId());

    }

    /**
     * Utility method to create a fresh set of test staff users in Keycloak.
     */
    protected void setUpTestUsers() {
        UsersResource usersResource = keycloakService.getRealm().users();
        String[] users = new String[] {
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

            // Assign a role (ADMIN in the original code, possibly 'STAFF' is better?)
            String adminId = fetchRoleByNameService.getGroupId("ADMIN", true).orElseThrow();
            userRoleService.addUserToGroup(username, adminId);
        }
    }
}