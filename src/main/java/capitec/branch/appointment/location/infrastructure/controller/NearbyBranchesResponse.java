package capitec.branch.appointment.location.infrastructure.controller;

import capitec.branch.appointment.location.app.NearbyBranchDTO;
import capitec.branch.appointment.sharekernel.Pagination;

import java.util.List;

/**
 * Response wrapper for nearby branches search results.
 */
public record NearbyBranchesResponse(
        List<NearbyBranchDTO> branches,
        Pagination pagination
) {
}
