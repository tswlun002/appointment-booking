package capitec.branch.appointment.location.infrastructure.controller;

import capitec.branch.appointment.location.app.NearbyBranchDTO;
import capitec.branch.appointment.sharekernel.Pagination;

import java.util.List;

/**
 * Response wrapper for branch search results by area.
 *
 * @param branches   list of branches matching the search criteria
 */
public record BranchSearchResponse(
        List<NearbyBranchDTO> branches,
        Pagination pagination
) {
}
