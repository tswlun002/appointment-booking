package capitec.branch.appointment.location.app;

import capitec.branch.appointment.location.domain.BranchLocation;
import capitec.branch.appointment.exeption.BranchLocationServiceException;
import capitec.branch.appointment.location.domain.Coordinates;
import capitec.branch.appointment.utils.UseCase;
import com.github.benmanes.caffeine.cache.Cache;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static capitec.branch.appointment.location.infrastructure.api.CapitecBranchLocationFetcher.BRANCH_LOCATIONS_BY_COORDINATES_CACHE;

@Slf4j
@UseCase
@Validated
@RequiredArgsConstructor
public class FindNearestBranchesUseCase {

    private static final double NEARBY_RADIUS_KM = 5.0;

    private final BranchLocationFetcher branchLocationFetcher;
    private final CacheManager cacheManager;

    public List<NearbyBranchDTO> execute(@Valid FindNearestBranchesQuery query) {
        log.info("Finding nearest branches for coordinates: lat={}, lon={}", query.latitude(), query.longitude());

        Coordinates customerLocation = new Coordinates(query.latitude(), query.longitude());

        try {
            List<BranchLocation> nearestBranches = findNearestBranches(customerLocation, query);

            log.info("Found {} nearby branches", nearestBranches.size());

            return mapToDto(nearestBranches, customerLocation, false);

        } catch (BranchLocationServiceException e) {
            log.warn("Branch location service unavailable, attempting cache fallback: {}", e.getMessage());
            return handleFallback(customerLocation, query);
        }
    }

    private List<BranchLocation> findNearestBranches(Coordinates customerLocation, FindNearestBranchesQuery query) {
        List<BranchLocation> branches = branchLocationFetcher.fetchByCoordinates(customerLocation);

        var stream = branches.stream()
                .filter(BranchLocation::isAvailableForBooking)
                .sorted(Comparator.comparingDouble(branch -> branch.distanceFrom(customerLocation)));

        if (query.maxDistanceKm() != null) {
            stream = stream.filter(branch -> branch.distanceFrom(customerLocation) <= query.maxDistanceKm());
        }

        return stream.limit(query.limit()).toList();
    }

    private List<NearbyBranchDTO> handleFallback(Coordinates customerLocation, FindNearestBranchesQuery query) {
        // First try exact cache key
        String exactCacheKey = buildCacheKey(customerLocation);
        List<BranchLocation> cachedBranches = getCachedBranches(exactCacheKey);

        if (cachedBranches != null && !cachedBranches.isEmpty()) {
            log.info("Returning {} cached branches for exact coordinates: {}", cachedBranches.size(), exactCacheKey);
            return mapToDto(filterAndLimit(cachedBranches, customerLocation, query), customerLocation, false);
        }

        // Try finding nearby cached coordinates within radius
        Optional<NearbyCache> nearbyCache = findNearbyCachedCoordinates(customerLocation);

        if (nearbyCache.isPresent()) {
            NearbyCache nearby = nearbyCache.get();
            log.info("Returning {} cached branches from nearby location ({} km away): {}",
                    nearby.branches.size(), String.format("%.2f", nearby.distance), nearby.cacheKey);
            return mapToDto(filterAndLimit(nearby.branches, customerLocation, query), customerLocation, true);
        }

        log.error("No cached data available for coordinates: {} or within {} km radius", exactCacheKey, NEARBY_RADIUS_KM);
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Branch locator service is temporarily unavailable. Please try again later.");
    }

    private Optional<NearbyCache> findNearbyCachedCoordinates(Coordinates customerLocation) {
        org.springframework.cache.Cache springCache = cacheManager.getCache(BRANCH_LOCATIONS_BY_COORDINATES_CACHE);
        if (springCache == null) {
            return Optional.empty();
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
                    double distance = customerLocation.distanceTo(cachedCoordinates);
                    @SuppressWarnings("unchecked")
                    List<BranchLocation> branches = (List<BranchLocation>) entry.getValue();
                    return new NearbyCache(cacheKey, cachedCoordinates, branches, distance);
                })
                .filter(nearby -> nearby != null && nearby.distance <= NEARBY_RADIUS_KM)
                .filter(nearby -> nearby.branches != null && !nearby.branches.isEmpty())
                .min(Comparator.comparingDouble(nearby -> nearby.distance));
    }

    private String buildCacheKey(Coordinates coordinates) {
        return coordinates.latitude() + "_" + coordinates.longitude();
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

    @SuppressWarnings("unchecked")
    private List<BranchLocation> getCachedBranches(String cacheKey) {
        org.springframework.cache.Cache cache = cacheManager.getCache(BRANCH_LOCATIONS_BY_COORDINATES_CACHE);
        if (cache == null) {
            return null;
        }
        org.springframework.cache.Cache.ValueWrapper valueWrapper = cache.get(cacheKey);
        return valueWrapper != null ? (List<BranchLocation>) valueWrapper.get() : null;
    }

    private List<BranchLocation> filterAndLimit(List<BranchLocation> branches, Coordinates customerLocation, FindNearestBranchesQuery query) {
        var stream = branches.stream()
                .filter(BranchLocation::isAvailableForBooking)
                .sorted(Comparator.comparingDouble(branch -> branch.distanceFrom(customerLocation)));

        if (query.maxDistanceKm() != null) {
            stream = stream.filter(branch -> branch.distanceFrom(customerLocation) <= query.maxDistanceKm());
        }

        return stream.limit(query.limit()).toList();
    }

    private List<NearbyBranchDTO> mapToDto(List<BranchLocation> branches, Coordinates customerLocation, boolean fromNearbyCache) {
        return branches.stream()
                .map(branch -> NearbyBranchDTO.from(branch, branch.distanceFrom(customerLocation), fromNearbyCache))
                .toList();
    }

    private record NearbyCache(String cacheKey, Coordinates coordinates, List<BranchLocation> branches, double distance) {}
}

