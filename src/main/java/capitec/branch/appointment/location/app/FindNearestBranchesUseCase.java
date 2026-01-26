package capitec.branch.appointment.location.app;

import capitec.branch.appointment.location.domain.BranchLocation;
import capitec.branch.appointment.exeption.BranchLocationServiceException;
import capitec.branch.appointment.location.domain.BranchLocationFetcher;
import capitec.branch.appointment.location.domain.Coordinates;
import capitec.branch.appointment.utils.UseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;
import java.util.Comparator;
import java.util.List;

@Slf4j
@UseCase
@Validated
@RequiredArgsConstructor
public class FindNearestBranchesUseCase {

    private static final double NEARBY_RADIUS_KM = 5.0;

    private final BranchLocationFetcher branchLocationFetcher;
    private final GetNearestCachedBranch getNearestCachedBranch;

    public List<NearbyBranchDTO> execute(@Valid FindNearestBranchesQuery query) {
        log.info("Finding nearest branches for coordinates: lat={}, lon={}", query.latitude(), query.longitude());

        Coordinates customerLocation = new Coordinates(query.latitude(), query.longitude());

        try {
            List<BranchLocation> nearestBranches = findNearestBranches(customerLocation, query);

            log.info("Found {} nearby branches", nearestBranches.size());

            return mapToDto(nearestBranches, customerLocation, false);

        } catch (BranchLocationServiceException e) {
            log.warn("Branch location service unavailable, attempting cache fallback: {}", e.getMessage());
            return handleFallback(customerLocation,query);
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

    private List<NearbyBranchDTO> handleFallback(Coordinates customerLocation,FindNearestBranchesQuery query) {

        List<BranchLocation> nearByBranches = getNearestCachedBranch.findNearByBranches(customerLocation, NEARBY_RADIUS_KM);

        if (nearByBranches.isEmpty()) {
            String exactCacheKey = customerLocation.latitude() + "_" + customerLocation.longitude();

            log.error("No cached data available for coordinates: {} or within {} km radius", exactCacheKey, NEARBY_RADIUS_KM);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Branch locator service is temporarily unavailable. Please try again later.");
        }
        List<BranchLocation> branches = filterAndLimit(nearByBranches, customerLocation, query);
        return mapToDto(branches,customerLocation, true);
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

}

