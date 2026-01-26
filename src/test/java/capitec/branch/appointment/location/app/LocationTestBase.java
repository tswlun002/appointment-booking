package capitec.branch.appointment.location.app;

import capitec.branch.appointment.AppointmentBookingApplicationTests;
import capitec.branch.appointment.location.infrastructure.api.CapitecBranchLocationFetcher;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

/**
 * Base class for Location Use Case tests.
 * Sets up WireMock for Capitec Branch API.
 */
abstract class LocationTestBase extends AppointmentBookingApplicationTests {

    @Autowired
    protected FindNearestBranchesUseCase findNearestBranchesUseCase;

    @Autowired
    protected SearchBranchesByAreaUseCase searchBranchesByAreaUseCase;

    protected WireMock capitecApiWireMock;
    protected static final String CAPITEC_BRANCH_API_RESPONSE = capitecApiBranchResponse();
    protected static final String EMPTY_BRANCH_RESPONSE = capitecApiBranchEmptyResponse();
    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;
    @Autowired
    @Qualifier("branchLocationCacheManager")
    private CacheManager cacheManager;

    @BeforeEach
    void setupLocationBase() {
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
                cacheManager.getCache(CapitecBranchLocationFetcher.BRANCH_LOCATIONS_BY_COORDINATES_CACHE),
                cacheManager.getCache(CapitecBranchLocationFetcher.BRANCH_LOCATIONS_BY_AREA_CACHE)
        };
        for (Cache cache1 : cache) {
            if(cache1!=null) {
                cache1.clear();
            }
        }
    }

}

