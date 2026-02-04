package capitec.branch.appointment.location.infrastructure.controller;

import capitec.branch.appointment.location.app.FindNearestBranchesQuery;
import capitec.branch.appointment.location.app.FindNearestBranchesUseCase;
import capitec.branch.appointment.location.app.NearbyBranchDTO;
import capitec.branch.appointment.location.app.SearchBranchesByAreaQuery;
import capitec.branch.appointment.location.app.SearchBranchesByAreaUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for branch location operations.
 * Provides endpoints to search branches by area text or find nearest branches by coordinates.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/locations/branches")
@RequiredArgsConstructor
@Validated
public class BranchLocationController {

    private final SearchBranchesByAreaUseCase searchBranchesByAreaUseCase;
    private final FindNearestBranchesUseCase findNearestBranchesUseCase;

    /**
     * Search branches by area text input.
     * Searches across city, province, suburb, address, postal code, and branch name.
     *
     * @param searchText the search query text
     * @param traceId    unique trace identifier for request tracking
     * @return list of branches matching the search criteria
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('app_user')")
    public ResponseEntity<BranchSearchResponse> searchBranchesByArea(
            @RequestParam("searchText")
            String searchText,
            @RequestHeader("Trace-Id") String traceId
    ) {
        log.info("Searching branches by area: '{}', traceId: {}", searchText, traceId);

        SearchBranchesByAreaQuery query = new SearchBranchesByAreaQuery(searchText);
        List<NearbyBranchDTO> branches = searchBranchesByAreaUseCase.execute(query);

        log.info("Found {} branches for search '{}', traceId: {}", branches.size(), searchText, traceId);

        return ResponseEntity.ok(new BranchSearchResponse(branches, branches.size()));
    }

    /**
     * Find nearest branches by geographic coordinates.
     * Returns branches sorted by distance from the provided coordinates.
     *
     * @param latitude      latitude of the customer's location
     * @param longitude     longitude of the customer's location
     * @param limit         maximum number of branches to return (default 10)
     * @param maxDistanceKm maximum distance in kilometers to search
     * @param traceId       unique trace identifier for request tracking
     * @return list of nearby branches sorted by distance
     */
    @GetMapping("/nearby")
    @PreAuthorize("hasAnyRole('app_user')")
    public ResponseEntity<NearbyBranchesResponse> findNearestBranches(
            @RequestParam("latitude")
            Double latitude,

            @RequestParam("longitude")
            Double longitude,

            @RequestParam(value = "limit", required = false,defaultValue = "10")
            Integer limit,

            @RequestParam(value = "maxDistanceKm", required = false)
            Double maxDistanceKm,

            @RequestHeader("Trace-Id") String traceId
    ) {
        log.info("Finding nearest branches for lat={}, lon={}, limit={}, maxDistanceKm={}, traceId: {}",
                latitude, longitude, limit, maxDistanceKm, traceId);

        FindNearestBranchesQuery query = new FindNearestBranchesQuery(latitude, longitude, limit, maxDistanceKm);
        List<NearbyBranchDTO> branches = findNearestBranchesUseCase.execute(query);

        log.info("Found {} nearby branches for coordinates ({}, {}), traceId: {}",
                branches.size(), latitude, longitude, traceId);

        return ResponseEntity.ok(new NearbyBranchesResponse(branches, branches.size()));
    }
}
