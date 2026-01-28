package capitec.branch.appointment.location.infrastructure.controller;

import capitec.branch.appointment.location.app.NearbyBranchDTO;

import java.util.List;

/**
 * Response wrapper for nearby branches search results.
 *
 * @param branches   list of nearby branches sorted by distance
 * @param totalCount total number of branches in the response
 */
public record NearbyBranchesResponse(
        List<NearbyBranchDTO> branches,
        int totalCount
) {
}
