package capitec.branch.appointment.location.infrastructure.controller;

import capitec.branch.appointment.location.app.NearbyBranchDTO;

import java.util.List;

/**
 * Response wrapper for branch search results by area.
 *
 * @param branches   list of branches matching the search criteria
 * @param totalCount total number of branches in the response
 */
public record BranchSearchResponse(
        List<NearbyBranchDTO> branches,
        int totalCount
) {
}
