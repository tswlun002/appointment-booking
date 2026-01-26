package capitec.branch.appointment.location.infrastructure.api;

import capitec.branch.appointment.day.app.GetDateOfNextDaysQuery;
import capitec.branch.appointment.day.domain.Day;
import capitec.branch.appointment.exeption.BranchLocationServiceException;
import capitec.branch.appointment.location.app.GetNearestCachedBranch;
import capitec.branch.appointment.location.domain.BranchLocationFetcher;
import capitec.branch.appointment.location.domain.*;
import com.github.benmanes.caffeine.cache.Cache;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Supplier;


/**
 * Adapter to fetch branch locations from Capitec Branch Locator API.
 * Results are cached for 24 hours to reduce API calls.
 * Uses Circuit Breaker and Retry with exponential backoff for resilience.
 * Returns actual branches only (filters out ATMs).
 */
@Slf4j
@Service
public class CapitecBranchLocationFetcher implements BranchLocationFetcher, GetNearestCachedBranch {

    public static final String BRANCH_LOCATIONS_BY_COORDINATES_CACHE = "branchLocationsByCoordinates";
    public static final String BRANCH_LOCATIONS_BY_AREA_CACHE = "branchLocationsByArea";
    public static final String CACHE_MANAGER = "branchLocationCacheManager";
    public final GetDateOfNextDaysQuery getDateOfNextDaysQuery;

    private final RestClient restClient;
    private final String branchApiUrl;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final CacheManager cacheManager;

    public CapitecBranchLocationFetcher(
            RestClient.Builder restClientBuilder,
            @Value("${capitec.branch-locator-api.url}") String branchApiUrl,
            CircuitBreaker branchLocatorCircuitBreaker,
            Retry branchLocatorRetry, @Qualifier(CACHE_MANAGER) CacheManager cacheManager,
            GetDateOfNextDaysQuery getDateOfNextDaysQuery ) {
        this.restClient = restClientBuilder.build();
        this.branchApiUrl = branchApiUrl;
        this.circuitBreaker = branchLocatorCircuitBreaker;
        this.retry = branchLocatorRetry;
        this.cacheManager = cacheManager;
        this.getDateOfNextDaysQuery = getDateOfNextDaysQuery;

        // Log circuit breaker state changes
        circuitBreaker.getEventPublisher()
                .onStateTransition(event -> log.warn("Circuit breaker state changed: {}", event.getStateTransition()));
    }

    @Override
    @Cacheable(value = BRANCH_LOCATIONS_BY_COORDINATES_CACHE,cacheManager = CACHE_MANAGER, key = "#coordinates.latitude() + '_' + #coordinates.longitude()")
    public List<BranchLocation> fetchByCoordinates(Coordinates coordinates) {
        log.info("Fetching branches by coordinates: lat={}, lon={}", coordinates.latitude(), coordinates.longitude());

        var request = new CapitecBranchApiRequest.CoordinatesSearchRequest(
                coordinates.latitude(),
                coordinates.longitude()
        );

        return executeWithResilience(() -> fetchBranches(request));
    }

    @Override
    @Cacheable(value = BRANCH_LOCATIONS_BY_AREA_CACHE,cacheManager = CACHE_MANAGER, key = "#searchText.toLowerCase()")
    public List<BranchLocation> fetchByArea(String searchText) {
        log.info("Fetching branches by area: {}", searchText);

        var request = new CapitecBranchApiRequest.AreaSearchRequest(searchText);

        return executeWithResilience(() -> fetchBranches(request));
    }



    /**
     * Executes the supplier with retry and circuit breaker protection.
     * Order: Retry wraps CircuitBreaker wraps actual call (Retry → CB → Call)
     * This means:
     * - If call fails → retry with exponential backoff + jitter
     * - If circuit is OPEN → CB throws CallNotPermittedException
     * - CallNotPermittedException is ignored by Retry (configured in RetryConfig)
     * - Result: Retries STOP immediately when circuit breaker opens
     */
    private List<BranchLocation> executeWithResilience(Supplier<List<BranchLocation>> supplier) {
        // Order matters: Retry → CircuitBreaker → Call
        // 1. CircuitBreaker wraps the actual call
        Supplier<List<BranchLocation>> circuitBreakerSupplier = CircuitBreaker.decorateSupplier(circuitBreaker, supplier);
        // 2. Retry wraps CircuitBreaker (stops retrying on CallNotPermittedException)
        Supplier<List<BranchLocation>> retrySupplier = Retry.decorateSupplier(retry, circuitBreakerSupplier);

        try {
            return retrySupplier.get();
        } catch (CallNotPermittedException e) {
            log.error("Circuit breaker is open, request rejected without retry: {}", e.getMessage());
            throw new BranchLocationServiceException("Branch locator service is temporarily unavailable. Please try again later.", e);
        } catch (Exception e) {
            log.error("Failed to fetch branches after retries: {}", e.getMessage(), e);
            throw new BranchLocationServiceException("Unable to fetch branches. Please try again.", e);
        }
    }

    private List<BranchLocation> fetchBranches(CapitecBranchApiRequest request) {
        CapitecBranchApiResponse response = restClient.post()
                .uri(branchApiUrl + "/Branch")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(CapitecBranchApiResponse.class);

        if (response == null || response.branches() == null) {
            log.warn("Empty response from Capitec Branch API");
            return Collections.emptyList();
        }

        List<BranchLocation> branches = response.branches().stream()
                .filter(CapitecBranchApiResponse.CapitecBranchDto::isActualBranch)
                .map(branch->ApiToDomainMapper.mapToDomain(branch, getDateOfTheWeek()))
                .filter(Objects::nonNull)
                .toList();

        log.info("Fetched {} actual branches (filtered out ATMs)", branches.size());
        return branches;
    }
   private  Set<Day> getDateOfTheWeek(){
       return getDateOfNextDaysQuery.execute(DayOfWeek.MONDAY, DayOfWeek.SUNDAY);
   }


    @Override
    public  List<BranchLocation> findNearByBranches(Coordinates customerLocation, double nearbyRadiusKM){
        // Try finding nearby cached coordinates within radius
        return findNearbyCachedCoordinates(customerLocation,nearbyRadiusKM);


    }
    private List<BranchLocation> findNearbyCachedCoordinates(Coordinates customerLocation,double nearbyRadiusKM) {
        org.springframework.cache.Cache springCache = cacheManager.getCache(BRANCH_LOCATIONS_BY_COORDINATES_CACHE);
        if (springCache == null) {
            return Collections.emptyList();
        }

        // Get underlying Caffeine cache to access all keys
        Cache<Object, Object> caffeineCache = ((CaffeineCache) springCache).getNativeCache();
        Map<Object, Object> cacheMap = caffeineCache.asMap();

        return cacheMap.entrySet().stream()
                .map(entry -> {
                    String cacheKey = (String) entry.getKey();
                    Coordinates cachedCoordinates = parseCacheKey(cacheKey);
                    if (cachedCoordinates == null) {
                        return null;
                    }
                    //double distance = customerLocation.distanceTo(cachedCoordinates);

                    return (List<BranchLocation>) entry.getValue();

                })
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(nearby ->nearby.distanceFrom(customerLocation) <= nearbyRadiusKM)
                .toList();
    }

    private Coordinates parseCacheKey(String cacheKey) {
        try {
            String[] parts = cacheKey.split("_");
            if (parts.length != 2) {
                return null;
            }
            return new Coordinates(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]));
        } catch (Exception e) {
            log.debug("Failed to parse cache key: {}", cacheKey);
            return null;
        }
    }


}

