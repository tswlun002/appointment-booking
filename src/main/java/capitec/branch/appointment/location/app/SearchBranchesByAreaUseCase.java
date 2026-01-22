package capitec.branch.appointment.location.app;

import capitec.branch.appointment.location.domain.BranchLocation;
import capitec.branch.appointment.exeption.BranchLocationServiceException;
import capitec.branch.appointment.utils.UseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static capitec.branch.appointment.location.infrastructure.api.CapitecBranchLocationFetcher.BRANCH_LOCATIONS_BY_AREA_CACHE;

/**
 * Use case to search branches by area text input.
 * Searches across city, province, suburb, address, and branch name.
 */
@Slf4j
@UseCase
@Validated
@RequiredArgsConstructor
public class SearchBranchesByAreaUseCase {

    private final BranchLocationFetcher branchLocationFetcher;
    private final CacheManager cacheManager;

    public List<NearbyBranchDTO> execute(@Valid SearchBranchesByAreaQuery query) {
        log.info("Searching branches by area: {}", query.searchText());

        try {
            List<BranchLocation> branches = searchByArea(query.searchText());

            log.info("Found {} branches matching '{}'", branches.size(), query.searchText());

            return mapToDto(branches);

        } catch (BranchLocationServiceException e) {
            log.warn("Branch location service unavailable, attempting cache fallback: {}", e.getMessage());
            return handleFallback(query.searchText());
        }
    }

    private List<BranchLocation> searchByArea(String searchText) {
        return branchLocationFetcher.fetchByArea(searchText).stream()
                .filter(BranchLocation::isAvailableForBooking)
                .toList();
    }

    private List<NearbyBranchDTO> handleFallback(String searchText) {
        String cacheKey = searchText.toLowerCase();
        List<BranchLocation> cachedBranches = getCachedBranches(cacheKey);

        if (cachedBranches != null && !cachedBranches.isEmpty()) {
            log.info("Returning {} cached branches for area: {}", cachedBranches.size(), searchText);
            return mapToDto(filterAvailable(cachedBranches));
        }

        log.error("No cached data available for area: {}", searchText);
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Branch locator service is temporarily unavailable. Please try again later.");
    }

    @SuppressWarnings("unchecked")
    private List<BranchLocation> getCachedBranches(String cacheKey) {
        Cache cache = cacheManager.getCache(BRANCH_LOCATIONS_BY_AREA_CACHE);
        if (cache == null) {
            return null;
        }
        Cache.ValueWrapper valueWrapper = cache.get(cacheKey);
        return valueWrapper != null ? (List<BranchLocation>) valueWrapper.get() : null;
    }

    private List<BranchLocation> filterAvailable(List<BranchLocation> branches) {
        return branches.stream()
                .filter(BranchLocation::isAvailableForBooking)
                .toList();
    }

    private List<NearbyBranchDTO> mapToDto(List<BranchLocation> branches) {
        return branches.stream()
                .map(branch -> NearbyBranchDTO.from(branch, 0))
                .toList();
    }
}

