package capitec.branch.appointment.branch.app;

import capitec.branch.appointment.AppointmentBookingApplicationTests;
import capitec.branch.appointment.branch.domain.Branch;
import capitec.branch.appointment.branch.infrastructure.dao.BranchDaoImpl;
import capitec.branch.appointment.location.infrastructure.api.CapitecBranchLocationFetcher;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Base class for all Branch Use Case tests, handling application setup and global cleanup.
 *
 * NOTE: We autowire the specific Use Cases for cleanup since the original BranchUseCase
 * has been split. We need AddBranchUseCase, DeleteBranchUseCase, and GetAllBranchesQuery.
 */
abstract class BranchTestBase extends AppointmentBookingApplicationTests {

    // We must autowire the new Use Case implementations for setup/cleanup
    @Autowired
    protected DeleteBranchUseCase deleteBranchUseCase;
    @Autowired
    protected GetBranchQuery getAllBranchesQuery;
    @Autowired
    @Qualifier("branchLocationCacheManager")
    protected CacheManager cacheManagerBranchLocationService;
    @Autowired
    @Qualifier("branchCacheManager")
    protected CacheManager cacheManagerBranches;
    @Autowired
    protected CircuitBreakerRegistry circuitBreakerRegistry;

    protected static final String CAPITEC_BRANCH_API_RESPONSE = capitecApiBranchResponse();
    protected static final String EMPTY_BRANCH_RESPONSE = capitecApiBranchEmptyResponse();
    protected WireMock capitecApiWireMock;

    @BeforeEach
    protected void setupWireMock() {
        capitecApiWireMock = new WireMock(
                wiremockContainer.getHost(),
                wiremockContainer.getFirstMappedPort()
        );
        // Reset any previous stubs
        capitecApiWireMock.resetMappings();
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
    
    @AfterEach
    public void tearDown() {
        // Collect IDs before deletion to avoid modifying the collection while iterating.
        List<String> branchIds = getAllBranchesQuery.execute(0,100)
                .branches()
                .stream()
                .map(Branch::getBranchId)
                .collect(Collectors.toList());

        // Use the DeleteBranchUseCase to ensure cleanup is done via the Use Case boundary.
        branchIds.forEach(deleteBranchUseCase::execute);
    }

    /**
     * Helper method to create a BranchDTO from a CsvSource line.
     */
    protected BranchDTO createBranchDTO(String branchId) {
        
        return new BranchDTO(branchId);
    }
}